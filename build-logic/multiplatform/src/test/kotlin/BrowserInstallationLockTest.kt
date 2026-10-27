import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS

class BrowserInstallationLockTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test(timeout = 60_000)
  fun `separate processes cannot install into the same cache concurrently`() {
    val cache = temporaryFolder.newFolder("shared browser cache")
    val java = File(System.getProperty("java.home"), "bin/java").path
    val classpath = listOf(javaClass, BrowserInstallationLock::class.java, Unit::class.java)
      .map { File(it.protectionDomain.codeSource.location.toURI()).path }
      .distinct().joinToString(File.pathSeparator)
    val processes = List(2) {
      ProcessBuilder(java, "-cp", classpath, "BrowserInstallationLockProbe", cache.path).redirectErrorStream(true).start()
    }
    processes.forEach { process ->
      if (!process.waitFor(30, SECONDS)) {
        process.destroyForcibly()
        fail("Concurrent installer process timed out")
      }
      assertEquals(process.inputStream.bufferedReader().readText(), 0, process.exitValue())
    }
    assertEquals("installed\ninstalled\n", cache.resolve("installations").readText())
  }
}

/** Uses separate JVMs so the test exercises the production OS lock as well as closing it between invocations. */
object BrowserInstallationLockProbe {
  @JvmStatic
  fun main(args: Array<String>) {
    val cache = File(args[0])
    cache.resolve("started-${ProcessHandle.current().pid()}").writeText("")
    BrowserInstallationLock.withLock(cache) {
      val deadline = System.nanoTime() + 5_000_000_000L
      while (cache.listFiles().orEmpty().count { it.name.startsWith("started-") } < 2) {
        check(System.nanoTime() < deadline) { "Other installer process did not start" }
        Thread.sleep(10)
      }
      val active = cache.resolve("active")
      Files.createFile(active.toPath())
      try {
        Thread.sleep(200)
        cache.resolve("installations").appendText("installed\n")
      } finally {
        active.delete()
      }
    }
  }
}
