import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit.SECONDS
import java.nio.file.Files

class ManagedChromeInstallerTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test(timeout = 60_000)
  fun `concurrent processes share one revision and incomplete installations are rebuilt`() {
    val cache = temporaryFolder.newFolder("shared browser cache")
    val version = "156.0.8078.47"
    val java = File(System.getProperty("java.home"), "bin/java").path
    val classpath = listOf(javaClass, ManagedChromeInstaller::class.java, Unit::class.java)
      .map { File(it.protectionDomain.codeSource.location.toURI()).path }
      .distinct().joinToString(File.pathSeparator)
    val processes = List(2) {
      ProcessBuilder(java, "-cp", classpath, "ManagedChromeInstallerProbe", cache.path, version).redirectErrorStream(true).start()
    }
    processes.forEach { process ->
      if (!process.waitFor(30, SECONDS)) {
        process.destroyForcibly()
        fail("Concurrent installer process timed out")
      }
      assertEquals(process.inputStream.bufferedReader().readText(), 0, process.exitValue())
    }
    assertEquals("$version\n", cache.resolve("downloads").readText())
    assertFalse(cache.resolve("active").exists())

    val nextVersion = "156.0.8078.48"
    try {
      ManagedChromeInstaller.executable(cache, "test-platform", nextVersion) { directory ->
        directory.resolve("browser").writeText("partial executable")
        directory.resolve("incomplete").writeText("interrupted extraction")
        throw IOException("interrupted download")
      }
      fail("The interrupted installation must fail")
    } catch (expected: IOException) {
      assertEquals("interrupted download", expected.message)
    }
    val repaired = ManagedChromeInstaller.executable(cache, "test-platform", nextVersion) { directory ->
      assertFalse("The partial installation must be removed before retrying", directory.resolve("incomplete").exists())
      directory.resolve("runtime-file").writeText("complete")
      directory.resolve("browser").apply { writeText("complete executable") }
    }
    assertTrue(repaired.parentFile.resolve("runtime-file").isFile)
    assertEquals(repaired, ManagedChromeInstaller.executable(cache, "test-platform", nextVersion) { error("Completed revision was downloaded again") })
    ManagedChromeInstaller.executable(cache, "test-platform", version) { error("Switching back to the old revision must reuse it") }

    val relocatedCache = File(cache.parentFile, "relocated browser cache")
    Files.move(cache.toPath(), relocatedCache.toPath())
    val relocatedBrowser = ManagedChromeInstaller.executable(relocatedCache, "test-platform", nextVersion) { error("Relocating the cache must reuse completed revisions") }
    assertEquals(repaired.relativeTo(cache).path, relocatedBrowser.relativeTo(relocatedCache).path)
  }
}
