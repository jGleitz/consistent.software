import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KarmaChromeIdentityTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test(timeout = 600_000)
  fun `browser test inputs cache the discovered browser and invalidate its changed version`() {
    val project = fixture()
    assertFalse(project.resolve("build/js/node_modules").exists())

    val first = run(project)
    assertEquals(SUCCESS, first.task(":kotlinNpmInstall")?.outcome)
    assertEquals(SUCCESS, first.task(":jsBrowserTest")?.outcome)
    assertTrue(project.resolve("build/js/node_modules/karma-chrome-launcher/index.js").isFile)
    assertResultWritten(project)
    assertEquals(listOf("executed"), executions(project))

    val unchanged = run(project)
    assertReusedConfigurationCache(unchanged)
    assertEquals(UP_TO_DATE, unchanged.task(":jsBrowserTest")?.outcome)

    if (System.getProperty("os.name").startsWith("Windows")) {
      project.resolve("default-browser.exe").copyTo(project.resolve("relocated-browser.exe"))
      val relocated = run(project, extraEnvironment = mapOf("TEST_BROWSER_FILENAME" to "relocated-browser.exe"))
      assertReusedConfigurationCache(relocated)
      assertEquals(UP_TO_DATE, relocated.task(":jsBrowserTest")?.outcome)
      assertEquals(listOf("executed"), executions(project))
    }

    project.resolve("build/mock-browser-result.txt").delete()
    val cached = run(project)
    assertReusedConfigurationCache(cached)
    assertEquals(FROM_CACHE, cached.task(":jsBrowserTest")?.outcome)
    assertEquals(listOf("executed"), executions(project))

    val updated = run(project, defaultVersion = "Chrome 2")
    assertReusedConfigurationCache(updated)
    assertEquals(SUCCESS, updated.task(":jsBrowserTest")?.outcome)
    assertResultWritten(project)
    assertEquals(listOf("executed", "executed"), executions(project))
  }

  @Test(timeout = 600_000)
  fun `browser test inputs honour the test task environment over the inherited environment`() {
    val project = fixture()
    val explicitEnvironment = mapOf(
      "CHROME_BIN" to "inherited-chrome",
      "TEST_TASK_CHROME_BIN" to "task-chrome",
    )

    val first = run(project, extraEnvironment = explicitEnvironment)
    assertEquals(SUCCESS, first.task(":jsBrowserTest")?.outcome)
    assertResultWritten(project)
    assertEquals(listOf("executed"), executions(project))

    val unusedBrowserChanged = run(project, defaultVersion = "Chrome 2", extraEnvironment = explicitEnvironment)
    assertReusedConfigurationCache(unusedBrowserChanged)
    assertEquals(UP_TO_DATE, unusedBrowserChanged.task(":jsBrowserTest")?.outcome)

    val selectedBrowserChanged =
      run(project, defaultVersion = "Chrome 2", explicitVersion = "Explicit Chrome 2", extraEnvironment = explicitEnvironment)
    assertReusedConfigurationCache(selectedBrowserChanged)
    assertEquals(SUCCESS, selectedBrowserChanged.task(":jsBrowserTest")?.outcome)
    assertEquals(listOf("executed", "executed"), executions(project))
  }

  @Test(timeout = 600_000)
  fun `an unidentified browser version prevents cache restoration`() {
    val project = fixture()
    val first = run(project, defaultVersion = "")
    assertEquals(SUCCESS, first.task(":jsBrowserTest")?.outcome)

    project.resolve("build/mock-browser-result.txt").delete()
    val repeated = run(project, defaultVersion = "")
    assertReusedConfigurationCache(repeated)
    assertEquals(SUCCESS, repeated.task(":jsBrowserTest")?.outcome)
    assertEquals(listOf("executed", "executed"), executions(project))

    val required = run(project, defaultVersion = "", requireVersion = true, expectFailure = true)
    val inputName = if (System.getProperty("os.name").startsWith("Windows")) "chromeExecutable" else "chromeVersion"
    assertTrue(required.output, required.output.contains(inputName))
    assertTrue(required.output, required.output.contains("doesn't have a configured value"))
  }

  private fun fixture(): File {
    val project = temporaryFolder.newFolder()
    val fixtureFiles = listOf(
      "mise.toml",
      "mise.lock",
      ".sdkmanrc",
      "gradle.properties",
      "gradle/testLibs.versions.toml",
      "gradle/kotlin-js-store/package-lock.json",
      "settings.gradle.kts",
      "build.gradle.kts",
    )
    for (path in fixtureFiles) copyResource("fixture/$path", project.resolve(path))
    copyResource("mock-browser-process.cjs", project.resolve("mock-browser-process.cjs"))
    project.resolve("src/jsTest/kotlin/Fixture.kt").apply {
      parentFile.mkdirs()
      writeText("fun fixture() = Unit")
    }
    return project
  }

  private fun copyResource(path: String, target: File) {
    target.parentFile.mkdirs()
    resource(path).use { input -> target.outputStream().use { input.copyTo(it) } }
  }

  private fun resource(path: String) = checkNotNull(javaClass.getResourceAsStream("/$path")) { "Missing test resource: $path" }

  private fun run(
    project: File,
    defaultVersion: String = "Chrome 1",
    explicitVersion: String = "Explicit Chrome 1",
    extraEnvironment: Map<String, String> = emptyMap(),
    requireVersion: Boolean = false,
    expectFailure: Boolean = false,
  ): BuildResult {
    // Windows fingerprints the file; Unix probes the reported version. Neither requires an installed browser.
    for ((name, version) in mapOf("default-browser.exe" to defaultVersion, "explicit-browser.exe" to explicitVersion)) {
      project.resolve(name).apply { if (version.isEmpty()) delete() else writeText(version) }
    }
    return GradleRunner.create()
      .withProjectDir(project)
      .withTestKitDir(File(System.getProperty("test.gradleUserHome")))
      .withPluginClasspath()
      .withEnvironment(
        System.getenv().filterKeys { !it.equals("CHROME_BIN", true) && it != "TEST_TASK_CHROME_BIN" } +
          mapOf(
            "MISE_TRUSTED_CONFIG_PATHS" to project.absolutePath,
            "TEST_DEFAULT_CHROME_VERSION" to defaultVersion,
            "TEST_EXPLICIT_CHROME_VERSION" to explicitVersion,
          ) + extraEnvironment,
      )
      .withArguments("jsBrowserTest", "-PrequireChromeIdentity=$requireVersion", "--configuration-cache", "--build-cache", "--max-workers=2", "--stacktrace")
      .forwardOutput()
      .let { if (expectFailure) it.buildAndFail() else it.build() }
  }

  private fun assertReusedConfigurationCache(result: BuildResult) {
    assertTrue(result.output, result.output.contains("Reusing configuration cache."))
  }

  private fun assertResultWritten(project: File) {
    assertEquals("mock browser result", project.resolve("build/mock-browser-result.txt").readText())
  }

  private fun executions(project: File): List<String> = project.resolve("browser-executions.txt").readLines()
}
