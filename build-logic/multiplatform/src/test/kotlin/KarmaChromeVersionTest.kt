import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KarmaChromeVersionTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val windows = System.getProperty("os.name").startsWith("Windows")
  private val macOs = System.getProperty("os.name").startsWith("Mac")

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

    val unchanged = run(project)
    assertReusedConfigurationCache(unchanged)
    assertEquals(TaskOutcome.UP_TO_DATE, unchanged.task(":jsBrowserTest")?.outcome)

    project.resolve("build/test-results/jsBrowserTest").deleteRecursively()
    val cached = run(project)
    assertReusedConfigurationCache(cached)
    assertEquals(TaskOutcome.FROM_CACHE, cached.task(":jsBrowserTest")?.outcome)
    assertEquals(listOf("default"), launches(project))

    val updated = run(project, defaultVersion = "Chrome 2")
    assertReusedConfigurationCache(updated)
    assertEquals(TaskOutcome.SUCCESS, updated.task(":jsBrowserTest")?.outcome)
    assertBrowserTestPassed(project)
    assertEquals(listOf("default", "default"), launches(project))
  }

  @Test
  fun `real browser tests honour the test task environment over the inherited environment`() {
    val project = fixture()
    val explicitEnvironment = mapOf(
      "CHROME_BIN" to defaultBrowser(project).absolutePath,
      "TEST_TASK_CHROME_BIN" to explicitBrowser(project).absolutePath,
    )

    val first = run(project, extraEnvironment = explicitEnvironment)
    assertEquals(TaskOutcome.SUCCESS, first.task(":jsBrowserTest")?.outcome)
    assertBrowserTestPassed(project)
    assertEquals(listOf("explicit"), launches(project))

    val unusedBrowserChanged = run(project, defaultVersion = "Chrome 2", extraEnvironment = explicitEnvironment)
    assertReusedConfigurationCache(unusedBrowserChanged)
    assertEquals(TaskOutcome.UP_TO_DATE, unusedBrowserChanged.task(":jsBrowserTest")?.outcome)

    val selectedBrowserChanged =
      run(project, defaultVersion = "Chrome 2", explicitVersion = "Explicit Chrome 2", extraEnvironment = explicitEnvironment)
    assertReusedConfigurationCache(selectedBrowserChanged)
    assertEquals(TaskOutcome.SUCCESS, selectedBrowserChanged.task(":jsBrowserTest")?.outcome)
    assertEquals(listOf("explicit", "explicit"), launches(project))
  }

  @Test
  fun `host browser command names resolve using only the supplied PATH`() {
    val project = fixture()
    val browser = defaultBrowser(project).absoluteFile
    val environment = mapOf(
      "CHROME_BIN" to if (windows) browser.nameWithoutExtension else browser.name,
      "PATH" to browser.parent,
      "PATHEXT" to ".EXE",
    )

    assertEquals(browser, installedBrowser(environment))
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
    val wrapper = if (windows) compileWindowsWrapper(project) else {
      project.resolve("wrapper/browser-wrapper").also { copyResource("browser-wrapper.sh", it) }
    }
    for (browser in listOf(defaultBrowser(project), explicitBrowser(project))) {
      browser.parentFile.mkdirs()
      wrapper.copyTo(browser)
      check(browser.setExecutable(true))
    }
    return project
  }

  private fun defaultBrowser(project: File): File = project.resolve(
    when {
      windows -> "browsers/Google/Chrome/Application/chrome.exe"
      macOs -> "browsers/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
      else -> "bin/google-chrome"
    },
  )

  private fun explicitBrowser(project: File) = project.resolve(if (windows) "bin/explicit-chrome.exe" else "bin/explicit-chrome")

  private fun compileWindowsWrapper(project: File): File {
    val source = project.resolve("wrapper/BrowserWrapper.cs")
    copyResource("BrowserWrapper.cs", source)
    val executable = project.resolve("wrapper/browser-wrapper.exe")
    val powershell = File(System.getenv("SystemRoot"), "System32/WindowsPowerShell/v1.0/powershell.exe")
    val process = ProcessBuilder(
      powershell.absolutePath,
      "-NoProfile",
      "-NonInteractive",
      "-Command",
      resource("build-browser-wrapper.ps1").bufferedReader().use { it.readText() },
    ).apply {
      environment()["TEST_WRAPPER_SOURCE"] = source.absolutePath
      environment()["TEST_WRAPPER_OUTPUT"] = executable.absolutePath
      redirectErrorStream(true)
    }.start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    check(process.waitFor() == 0) { "Compiling the Windows browser wrapper failed: $output" }
    return executable
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
  ): BuildResult =
    GradleRunner.create()
      .withProjectDir(project)
      .withTestKitDir(File(System.getProperty("test.gradleUserHome")))
      .withPluginClasspath()
      .withEnvironment(
        // Gradle, mise and npm still need the host tools. Browser selection itself is controlled by the fixture.
        System.getenv().filterKeys { !it.equals("CHROME_BIN", true) && !it.equals("PATH", true) && it != "TEST_TASK_CHROME_BIN" } +
          mapOf(
            "PATH" to "${project.resolve("bin")}${File.pathSeparator}${System.getenv().value("PATH")}",
            "MISE_TRUSTED_CONFIG_PATHS" to project.absolutePath,
            "TEST_DEFAULT_CHROME_VERSION" to defaultVersion,
            "TEST_EXPLICIT_CHROME_VERSION" to explicitVersion,
            "TEST_CHROME_EXECUTABLE" to installedBrowser().absolutePath,
            "TEST_CHROME_LAUNCH_LOG" to project.resolve("browser-launches.txt").absolutePath,
            "TEST_BROWSER_HOME" to project.resolve("browsers").absolutePath,
          ) + extraEnvironment,
      )
      .withArguments("jsBrowserTest", "--configuration-cache", "--build-cache", "--max-workers=2", "--stacktrace")
      .build()

  private fun installedBrowser(environment: Map<String, String> = System.getenv()): File {
    val explicit = environment.value("CHROME_BIN")?.takeIf { it.isNotBlank() }
    val commands = if (explicit != null) listOf(explicit) else {
      when {
        windows -> listOf("LOCALAPPDATA", "PROGRAMFILES", "PROGRAMFILES(X86)", "ProgramW6432")
          .mapNotNull { environment.value(it)?.let { directory -> "$directory/Google/Chrome/Application/chrome.exe" } } +
          listOf("chrome.exe", "chromium.exe")
        macOs -> listOf(
          "${environment.value("HOME")}/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
          "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        )
        else -> listOf("google-chrome", "google-chrome-stable", "chromium", "chromium-browser")
      }
    }
    for (command in commands) {
      val executable = File(command)
      val candidates = if (executable.isAbsolute || command.contains(File.separatorChar)) {
        listOf(executable)
      } else {
        val names = if (windows && executable.extension.isEmpty()) {
          listOf(command) + environment.value("PATHEXT").orEmpty().split(';').filter { it.isNotEmpty() }.map { command + it }
        } else listOf(command)
        environment.value("PATH").orEmpty().split(File.pathSeparator).flatMap { directory -> names.map { File(directory, it) } }
      }
      candidates.firstOrNull { it.isFile && it.canExecute() }?.let { return it.absoluteFile }
    }
    error("Browser integration tests require an installed Chrome or Chromium")
  }

  private fun Map<String, String>.value(name: String): String? =
    if (windows) entries.firstOrNull { it.key.equals(name, true) }?.value else get(name)

  private fun assertReusedConfigurationCache(result: BuildResult) {
    assertTrue(result.output, result.output.contains("Reusing configuration cache."))
  }

  private fun assertBrowserTestPassed(project: File) {
    val reports = project.resolve("build/test-results/jsBrowserTest").walkTopDown().filter { it.extension == "xml" }.toList()
    assertTrue(reports.any { it.readText().contains("runs in a real browser") })
  }

  private fun launches(project: File): List<String> = project.resolve("browser-launches.txt").readLines()
}
