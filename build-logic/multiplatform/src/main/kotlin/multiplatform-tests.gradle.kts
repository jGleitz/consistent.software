import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.util.Base64

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
        implementation(devNpm("@puppeteer/browsers", "3.2.6"))
        implementation(devNpm("proper-lockfile", "4.1.2"))
        // Puppeteer's optional peer makes browser downloads honour HTTP(S)_PROXY.
        implementation(devNpm("proxy-agent", "8.0.2"))
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
}

tasks.withType<KotlinTest>().configureEach {
  inputs.property("ignoreRunFailures", provider { ignoreRunFailures })
  outputs.doNotCacheIf("Test runner failures would be cached as passing") { ignoreRunFailures }
}

val managedChromeBootstrap =
  "data:text/javascript;base64,${Base64.getEncoder().encodeToString(
    checkNotNull(javaClass.getResource("/managed-chrome.mjs")).readBytes(),
  )}"

// KGP disables caching for JS tests without giving a reason (see KT-78586). We enable it and declare the inputs that KGP leaves
// out. Node modules are covered: the tasks already track the installed package-lock.json.
tasks.withType<KotlinJsTest>().configureEach {
  outputs.cacheIf { true }
  val providers = project.providers
  val chromeBootstrap = managedChromeBootstrap
  val nodeRuntime =
    rootProject.the<NodeJsExtension>().nodeExecutable.flatMap { node ->
      providers
        .exec { commandLine(node, "-p", "JSON.stringify({version:process.version,platform:process.platform,arch:process.arch})") }
        .standardOutput.asText
        .map { it.trim() }
    }
  inputs.property("nodeJsRuntime", nodeRuntime)
  inputs.files("karma.config.d", "webpack.config.d").withPropertyName("jsConfigDirectories").withPathSensitivity(PathSensitivity.RELATIVE)
  onTestFrameworkSet {
    if (this is KotlinKarma) {
      environment("CONSISTENT_CHROME_VERSION", providers.gradleProperty("chromeVersion").get())
      // Node starts only on a cache miss. A data URL keeps the bootstrap's location out of the task inputs.
      // Set CHROME_BIN inside that process, rather than fingerprinting its machine-specific path in environment.
      nodeJsArgs += listOf("--import", chromeBootstrap)
    }
  }
}
