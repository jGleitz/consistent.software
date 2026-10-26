import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit.MINUTES

class ManagedChromeTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test(timeout = 600_000)
  fun `managed browser installation is shared serialised and repairs incomplete revisions`() {
    val project = temporaryFolder.newFolder("npm fixture")
    for (path in listOf("mise.toml", "mise.lock", "gradle.properties", "gradle/testLibs.versions.toml", "settings.gradle.kts", "build.gradle.kts")) {
      val file = project.resolve(path)
      file.parentFile.mkdirs()
      resource("fixture/$path").use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    GradleRunner.create()
      .withProjectDir(project)
      .withTestKitDir(File(System.getProperty("test.gradleUserHome")))
      .withPluginClasspath()
      .withEnvironment(System.getenv() + ("MISE_TRUSTED_CONFIG_PATHS" to project.absolutePath))
      .withArguments("kotlinNpmInstall", "--max-workers=2", "--stacktrace")
      .forwardOutput()
      .build()

    val bootstrap = temporaryFolder.root.resolve("managed-chrome.mjs")
    bootstrap.writeBytes(resource("managed-chrome.mjs").use { it.readBytes() })
    val tests = temporaryFolder.root.resolve("managed-chrome.test.mjs")
    tests.writeBytes(resource("managed-chrome.test.mjs").use { it.readBytes() })
    val process =
      ProcessBuilder(System.getProperty("test.nodeExecutable"), "--test", tests.absolutePath)
        .directory(project.resolve("build/js"))
        .inheritIO()
        .apply { environment()["TEST_BOOTSTRAP"] = bootstrap.absolutePath }
        .start()
    if (!process.waitFor(5, MINUTES)) {
      process.destroyForcibly()
      error("Managed browser regression tests timed out")
    }
    assertEquals("Managed browser regression tests failed", 0, process.exitValue())
  }

  private fun resource(path: String) = checkNotNull(javaClass.getResourceAsStream("/$path")) { "Missing test resource $path" }
}
