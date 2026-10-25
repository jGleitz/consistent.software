import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KarmaChromeVersionTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `real browser tests cache the discovered browser and invalidate its changed version`() {
    val project = fixture()
    assertFalse(project.resolve("build/js/node_modules").exists())

    val first = run(project)
    assertEquals(TaskOutcome.SUCCESS, first.task(":kotlinNpmInstall")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, first.task(":jsBrowserTest")?.outcome)
    assertTrue(project.resolve("build/js/node_modules/karma-chrome-launcher/index.js").isFile)
    assertBrowserTestPassed(project)
    assertEquals(listOf("default"), launches(project))

    assertReused(run(project), TaskOutcome.UP_TO_DATE)
    project.resolve("build/test-results/jsBrowserTest").deleteRecursively()
    assertReused(run(project), TaskOutcome.FROM_CACHE)
    assertEquals(listOf("default"), launches(project))

    assertReused(run(project, defaultVersion = "Chrome 2"), TaskOutcome.SUCCESS)
    assertBrowserTestPassed(project)
    assertEquals(listOf("default", "default"), launches(project))
  }

  @Test
  fun `real browser tests honour the test task environment over the inherited environment`() {
    val project = fixture()
    val explicitEnvironment = mapOf(
      "CHROME_BIN" to project.resolve("bin/google-chrome").absolutePath,
      "TEST_TASK_CHROME_BIN" to project.resolve("bin/explicit-chrome").absolutePath,
    )

    assertEquals(TaskOutcome.SUCCESS, run(project, extraEnvironment = explicitEnvironment).task(":jsBrowserTest")?.outcome)
    assertBrowserTestPassed(project)
    assertEquals(listOf("explicit"), launches(project))

    assertReused(run(project, defaultVersion = "Chrome 2", extraEnvironment = explicitEnvironment), TaskOutcome.UP_TO_DATE)
    assertReused(
      run(project, defaultVersion = "Chrome 2", explicitVersion = "Explicit Chrome 2", extraEnvironment = explicitEnvironment),
      TaskOutcome.SUCCESS,
    )
    assertEquals(listOf("explicit", "explicit"), launches(project))
  }

  private fun fixture(): File {
    assumeTrue("The default browser lookup fixture uses Linux PATH discovery", System.getProperty("os.name") == "Linux")
    val project = temporaryFolder.newFolder()
    val fixtureFiles = listOf(
      "mise.toml",
      "mise.lock",
      ".sdkmanrc",
      "gradle.properties",
      "gradle/testLibs.versions.toml",
      "gradle/kotlin-js-store/package-lock.json",
    )
    for (path in fixtureFiles) {
      project.resolve(path).apply {
        parentFile.mkdirs()
        writeBytes(checkNotNull(this@KarmaChromeVersionTest.javaClass.getResourceAsStream("/fixture/$path")).use { it.readBytes() })
      }
    }
    project.resolve("settings.gradle").writeText(
      """
      rootProject.name = 'browser-cache-test'
      dependencyResolutionManagement {
        versionCatalogs { testLibs { from(files('gradle/testLibs.versions.toml')) } }
      }
      buildCache { local { directory = file('cache') } }
      """.trimIndent(),
    )
    project.resolve("build.gradle").writeText(
      """
      plugins {
        id 'nodejs'
        id 'multiplatform'
      }

      // Retain the repository's locked tooling versions while adapting workspace entries to this smaller fixture.
      def npm = extensions.getByType(org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension)
      npm.packageLockMismatchReport.set(org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport.NONE)
      npm.packageLockAutoReplace.set(true)

      kotlin.js {
        browser {
          testTask {
            def explicitBrowser = providers.environmentVariable('TEST_TASK_CHROME_BIN')
            if (explicitBrowser.isPresent()) environment('CHROME_BIN', explicitBrowser.get())
          }
        }
      }
      """.trimIndent(),
    )
    project.resolve("src/jsTest/kotlin/BrowserSpec.kt").apply {
      parentFile.mkdirs()
      writeText(
        """
        import io.kotest.core.spec.style.FunSpec

        class BrowserSpec : FunSpec({
          test("runs in a real browser") {
            check(js("typeof window !== 'undefined'") as Boolean)
          }
        })
        """.trimIndent(),
      )
    }
    browser(project, "google-chrome", "default", "TEST_DEFAULT_CHROME_VERSION")
    browser(project, "explicit-chrome", "explicit", "TEST_EXPLICIT_CHROME_VERSION")
    return project
  }

  private fun browser(project: File, name: String, label: String, versionVariable: String) {
    project.resolve("bin/$name").apply {
      parentFile.mkdirs()
      writeText(
        """
        #!/bin/sh
        if [ "${'$'}1" = "--version" ]; then
          printf '%s\n' "${'$'}$versionVariable"
        else
          printf '%s\n' '$label' >> "${'$'}TEST_CHROME_LAUNCH_LOG"
          exec "${'$'}TEST_CHROME_EXECUTABLE" "${'$'}@"
        fi
        """.trimIndent() + "\n",
      )
      check(setExecutable(true))
    }
  }

  private fun run(
    project: File,
    defaultVersion: String = "Chrome 1",
    explicitVersion: String = "Explicit Chrome 1",
    extraEnvironment: Map<String, String> = emptyMap(),
  ): BuildResult =
    GradleRunner.create()
      .withProjectDir(project)
      .withTestKitDir(File(System.getProperty("test.gradleUserHome")))
      .withPluginClasspath()
      .withEnvironment(
        System.getenv().filterKeys { it != "CHROME_BIN" && it != "TEST_TASK_CHROME_BIN" } + mapOf(
          "PATH" to "${project.resolve("bin")}${File.pathSeparator}${System.getenv("PATH")}",
          "MISE_TRUSTED_CONFIG_PATHS" to project.absolutePath,
          "TEST_DEFAULT_CHROME_VERSION" to defaultVersion,
          "TEST_EXPLICIT_CHROME_VERSION" to explicitVersion,
          "TEST_CHROME_EXECUTABLE" to installedBrowser().absolutePath,
          "TEST_CHROME_LAUNCH_LOG" to project.resolve("browser-launches.txt").absolutePath,
        ) + extraEnvironment,
      )
      .withArguments("jsBrowserTest", "--configuration-cache", "--build-cache", "--max-workers=2", "--stacktrace")
      .build()

  private fun installedBrowser(): File {
    System.getenv("CHROME_BIN")?.takeIf { it.isNotBlank() }?.let { return File(it) }
    for (command in listOf("google-chrome", "google-chrome-stable", "chromium", "chromium-browser")) {
      for (directory in System.getenv("PATH").split(File.pathSeparator)) {
        File(directory, command).takeIf { it.isFile && it.canExecute() }?.let { return it }
      }
    }
    error("Browser integration tests require an installed Chrome or Chromium")
  }

  private fun assertReused(result: BuildResult, outcome: TaskOutcome) {
    assertTrue(result.output, result.output.contains("Reusing configuration cache."))
    assertEquals(outcome, result.task(":jsBrowserTest")?.outcome)
  }

  private fun assertBrowserTestPassed(project: File) {
    val reports = project.resolve("build/test-results/jsBrowserTest").walkTopDown().filter { it.extension == "xml" }.toList()
    assertTrue(reports.any { it.readText().contains("runs in a real browser") })
  }

  private fun launches(project: File): List<String> = project.resolve("browser-launches.txt").readLines()
}
