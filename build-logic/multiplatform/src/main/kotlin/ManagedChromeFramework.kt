import groovy.json.JsonSlurper
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

/** Adds browser installation at execution time while retaining Karma's configuration, reporting and test execution. */
abstract class ManagedChromeFramework @Inject constructor(
  private val karma: KotlinJsTestFramework,
  private val browserVersion: String,
  private val cacheDirectory: File,
  private val npmDirectory: Provider<Directory>,
  private val nodeRuntime: Provider<String>,
) : KotlinJsTestFramework by karma {
  @get:Inject
  protected abstract val execOperations: ExecOperations

  // The nested type also fingerprints this convention plugin's implementation, including the installer.
  override val frameworkTaskInputs: Any
    get() = ManagedChromeInputs(karma.frameworkTaskInputs)

  override fun createTestExecutionSpec(
    task: KotlinJsTest,
    launchOpts: ProcessLaunchOptions,
    nodeJsArgs: MutableList<String>,
    debug: Boolean,
  ) = run {
    val runtime = JsonSlurper().parseText(nodeRuntime.get()) as Map<*, *>
    val platform = "${runtime["platform"]}-${runtime["arch"]}"
    val nodeExecutable = executable.get()
    val testWorkingDirectory = workingDir.get()
    val packageDirectory = npmDirectory.get().asFile.resolve("node_modules/@puppeteer/browsers")
    val metadata = JsonSlurper().parse(packageDirectory.resolve("package.json")) as Map<*, *>
    val bin = when (val entry = metadata["bin"]) {
      is String -> entry
      is Map<*, *> -> entry["browsers"] as String
      else -> error("@puppeteer/browsers has no CLI entry point")
    }
    val browser = ManagedChromeInstaller.executable(cacheDirectory, platform, browserVersion) { directory ->
      val output = ByteArrayOutputStream()
      execOperations.exec {
        commandLine(nodeExecutable, packageDirectory.resolve(bin), "install", "chrome@$browserVersion", "--path", directory, "--format", "{{path}}")
        workingDir(testWorkingDirectory)
        environment(launchOpts.environment.get())
        standardOutput = output
      }
      File(output.toString(UTF_8).trim())
    }
    // Launch-only state: never put the machine-specific path into KotlinJsTest.environment's @Input map.
    launchOpts.environment.put("CHROME_BIN", browser.path)
    karma.createTestExecutionSpec(task, launchOpts, nodeJsArgs, debug)
  }
}

class ManagedChromeInputs(@get:Nested @get:Optional val karma: Any?)
