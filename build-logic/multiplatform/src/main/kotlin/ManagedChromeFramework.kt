import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
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
  private val rootPackageDirectory: Provider<Directory>,
  private val browserDependencies: Set<RequiredKotlinJsDependency>,
) : KotlinJsTestFramework by karma {
  @get:Inject
  protected abstract val execOperations: ExecOperations

  // Gradle exposes transient rich-console status only through its internal progress API.
  @get:Inject
  protected abstract val progressLoggerFactory: ProgressLoggerFactory

  override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
    get() = karma.requiredNpmDependencies + browserDependencies

  // The nested type also fingerprints this convention plugin's implementation, including the installer.
  override val frameworkTaskInputs: Any
    get() = ManagedChromeInputs(karma.frameworkTaskInputs)

  override fun createTestExecutionSpec(
    task: KotlinJsTest,
    launchOpts: ProcessLaunchOptions,
    nodeJsArgs: MutableList<String>,
    debug: Boolean,
  ): TestExecutionSpec {
    val downloadOutput = ByteArrayOutputStream()
    task.withStatus("Preparing Chrome $browserVersion") {
      BrowserInstallationLock.withLock(cacheDirectory) {
        execOperations.exec {
          commandLine(
            "mise", "exec", "--",
            "npm", "exec", "--no", "--",
            "@puppeteer/browsers", "install", "chrome@$browserVersion", "--path", cacheDirectory, "--format", "{{path}}"
          )
          workingDir(rootPackageDirectory.get())
          environment(launchOpts.environment.get())
          standardOutput = downloadOutput
        }
      }
    }
    val browser = File(downloadOutput.toString(UTF_8).trim())
    // Launch-only state: never put the machine-specific path into KotlinJsTest.environment's @Input map.
    launchOpts.environment.put("CHROME_BIN", browser.path)
    return karma.createTestExecutionSpec(task, launchOpts, nodeJsArgs, debug)
  }

  private inline fun Task.withStatus(status: String, block: () -> Unit) {
    logger.info(status)
    val progress = progressLoggerFactory.newOperation(ManagedChromeFramework::class.java).start(status, status)
    try {
      block()
    } finally {
      progress.completed()
    }
  }
}

class ManagedChromeInputs(@get:Nested @get:Optional val karma: Any?)
