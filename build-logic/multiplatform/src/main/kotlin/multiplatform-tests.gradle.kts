import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.io.File

plugins {
  id("com.google.devtools.ksp")
  id("io.kotest")
}

kotlin {
  sourceSets {
    commonTest {
      dependencies {
        implementation(testLibs["kotest-framework"])
        implementation(testLibs["atrium-fluent"])
      }
    }

    // use named { it == "name"}.configureEach to configure the dependency only when the target is present, without requiring it.
    named { it == "jvmTest" }.configureEach {
      dependencies {
        runtimeOnly(testLibs["kotest-runner-junit5"])
      }
    }

    named { it == "jsTest" }.configureEach {
      dependencies {
        implementation(devNpm(buildLibs["npm-puppeteer-browsers"]))
        // Puppeteer's optional peer makes browser downloads honour HTTP(S)_PROXY.
        implementation(devNpm(buildLibs["npm-proxy-agent"]))
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.withType<AbstractTestTask>().configureEach {
  // Also invalidate entries written before individualTaskReports stopped KGP from ignoring test failures.
  inputs.property("ignoreFailures", provider { ignoreFailures })
  // With ignoreFailures, a task with failing tests succeeds. KGP’s allTests notices the failures only through test events, which
  // a result restored from the build cache does not replay, so caching such a result would turn the failure into a pass.
  outputs.doNotCacheIf("Failing tests would be cached as passing") { ignoreFailures }
  // A local UP-TO-DATE result would suppress the failure events, too.
  outputs.upToDateWhen { !ignoreFailures }
}

tasks.withType<KotlinTest>().configureEach {
  inputs.property("ignoreRunFailures", provider { ignoreRunFailures })
  outputs.doNotCacheIf("Test runner failures would be cached as passing") { ignoreRunFailures }
  outputs.upToDateWhen { !ignoreRunFailures }
}

// KGP disables caching for JS tests without giving a reason (see KT-78586). We enable it and declare the inputs that KGP leaves
// out. Node modules are covered: the tasks already track the installed package-lock.json.
tasks.withType<KotlinJsTest>().configureEach {
  outputs.cacheIf { true }
  val providers = project.providers
  val objects = project.objects
  val testTask = this
  inputs.files("karma.config.d", "webpack.config.d").withPropertyName("jsConfigDirectories").withPathSensitivity(PathSensitivity.RELATIVE)
  val version =
    project.buildLibs
      .findVersion("chrome")
      .get()
      .requiredVersion
  val npmDirectory = rootProject.the<NodeJsRootExtension>().rootPackageDirectory
  val cacheDirectory =
    providers.environmentVariable("CONSISTENT_BROWSER_CACHE").map(::File).orElse(
      providers
        .environmentVariable("XDG_CACHE_HOME")
        .orElse(providers.systemProperty("user.home").map { "$it/.cache" })
        .map { File(it, "consistent.software/browsers") },
    )
  onTestFrameworkSet {
    if (this is ManagedChromeFramework) return@onTestFrameworkSet
    // Probe the same provider KotlinJsTest uses for ProcessLaunchOptions.executable, including framework overrides.
    val nodeRuntime =
      executable.flatMap { node ->
        providers
          .exec {
            commandLine(node, "-p", "JSON.stringify({version:process.version,platform:process.platform,arch:process.arch})")
          }.standardOutput.asText
          .map { it.trim() }
      }
    testTask.inputs.property("nodeJsRuntimeIdentity", nodeRuntime)
    if (this is KotlinKarma) {
      testTask.dependsOn(":kotlinNpmInstall")
      testTask.inputs.property("chromeVersion", version)
      testTask.testFramework =
        objects.newInstance(
          ManagedChromeFramework::class.java,
          this,
          version,
          cacheDirectory.get(),
          npmDirectory,
          nodeRuntime,
        )
    }
  }
}
