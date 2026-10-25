import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KarmaChromeVersionTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `installs launcher before snapshotting and invalidates cached results after a browser update`() {
    val project = fixture()
    val browser = browser(project, "google-chrome", "Chrome 1")

    assertEquals(TaskOutcome.SUCCESS, run(project).task(":browserTest")?.outcome)
    assertEquals("Chrome 1", project.resolve("build/result.txt").readText())
    assertTrue(project.resolve("node_modules/karma-chrome-launcher/index.js").isFile)

    val unchanged = run(project)
    assertTrue(unchanged.output.contains("Reusing configuration cache."))
    assertEquals(TaskOutcome.UP_TO_DATE, unchanged.task(":browserTest")?.outcome)

    project.resolve("build/result.txt").delete()
    val cached = run(project)
    assertTrue(cached.output.contains("Reusing configuration cache."))
    assertEquals(TaskOutcome.FROM_CACHE, cached.task(":browserTest")?.outcome)

    browser.writeText("#!/bin/sh\nprintf '%s\\n' 'Chrome 2'\n")
    val updated = run(project)
    assertTrue(updated.output.contains("Reusing configuration cache."))
    assertEquals(TaskOutcome.SUCCESS, updated.task(":browserTest")?.outcome)
    assertEquals("Chrome 2", project.resolve("build/result.txt").readText())
  }

  @Test
  fun `explicit test environment overrides the default browser`() {
    val project = fixture()
    browser(project, "google-chrome", "Default Chrome")
    val explicitBrowser = browser(project, "explicit-chrome", "Explicit Chrome")

    assertEquals(TaskOutcome.SUCCESS, run(project, "-PexplicitBrowser=${explicitBrowser.absolutePath}").task(":browserTest")?.outcome)
    assertEquals("Explicit Chrome", project.resolve("build/result.txt").readText())
  }

  private fun fixture(): File {
    assumeFalse("The fixture browser is a POSIX shell script", System.getProperty("os.name").startsWith("Windows"))
    val project = temporaryFolder.newFolder()
    project.resolve("settings.gradle").writeText(
      """
      rootProject.name = 'browser-cache-test'
      buildCache { local { directory = file('cache') } }
      """.trimIndent(),
    )
    project.resolve("launcher").mkdirs()
    // A minimal installed launcher contract; real repository builds exercise the lockfile-pinned npm implementation.
    project.resolve("launcher/index.js").writeText(
      """
      function ChromeHeadless() {}
      ChromeHeadless.prototype = {
        ENV_CMD: 'CHROME_BIN',
        DEFAULT_CMD: { [process.platform]: 'google-chrome' }
      };
      module.exports = { 'launcher:ChromeHeadless': ['type', ChromeHeadless] };
      """.trimIndent(),
    )
    project.resolve("build.gradle").writeText(
      """
      plugins { id 'multiplatform-tests' apply false }

      def install = tasks.register('kotlinNpmInstall', Copy) {
        from 'launcher'
        into 'node_modules/karma-chrome-launcher'
      }

      @CacheableTask
      abstract class BrowserTestProbe extends DefaultTask {
        @Input abstract Property<String> getBrowserVersion()
        @OutputFile abstract RegularFileProperty getResult()
        @TaskAction void runTests() { result.get().asFile.text = browserVersion.get() }
      }

      def chromeVersion = providers.of(KarmaChromeVersion) {
        parameters.nodeExecutable.set('node')
        parameters.workingDirectory.set(layout.projectDirectory)
        parameters.environment.put('CHROME_BIN', providers.gradleProperty('explicitBrowser').orElse(''))
      }
      tasks.register('browserTest', BrowserTestProbe) {
        dependsOn install
        browserVersion.set(chromeVersion)
        result.set(layout.buildDirectory.file('result.txt'))
      }
      """.trimIndent(),
    )
    return project
  }

  private fun browser(project: File, name: String, version: String): File =
    project.resolve("bin/$name").apply {
      parentFile.mkdirs()
      writeText("#!/bin/sh\nprintf '%s\\n' '$version'\n")
      check(setExecutable(true))
    }

  private fun run(project: File, vararg extraArguments: String) =
    GradleRunner.create()
      .withProjectDir(project)
      .withPluginClasspath()
      .withEnvironment(
        System.getenv() + mapOf(
          "CHROME_BIN" to "",
          "PATH" to "${project.resolve("bin")}${File.pathSeparator}${System.getenv("PATH")}",
        ),
      )
      .withArguments("browserTest", "--configuration-cache", "--build-cache", "--stacktrace", *extraArguments)
      .build()
}
