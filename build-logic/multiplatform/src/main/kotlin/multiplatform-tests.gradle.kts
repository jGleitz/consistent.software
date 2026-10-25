import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

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

// KGP disables caching for JS tests without giving a reason (see KT-78586). We enable it and declare the inputs that KGP leaves
// out. Node modules are covered: the tasks already track the installed package-lock.json.
tasks.withType<KotlinJsTest>().configureEach {
  outputs.cacheIf { true }
  val providers = project.providers
  val nodeVersion =
    rootProject.the<NodeJsExtension>().nodeExecutable.flatMap { node ->
      providers
        .exec { commandLine(node, "--version") }
        .standardOutput.asText
        .map { it.trim() }
    }
  inputs.property("nodeJsVersion", nodeVersion)
  inputs.files("karma.config.d", "webpack.config.d").withPropertyName("jsConfigDirectories").withPathSensitivity(PathSensitivity.RELATIVE)
  onTestFrameworkSet {
    if (this is KotlinKarma) {
      // Karma’s Chrome launcher prefers $CHROME_BIN. Without it we cannot identify the browser, so keep caching disabled.
      val chromeVersion =
        providers.environmentVariable("CHROME_BIN").flatMap { chrome ->
          providers
            .exec { commandLine(chrome, "--version") }
            .standardOutput.asText
            .map { it.trim() }
        }
      inputs.property("chromeVersion", chromeVersion).optional(true)
      outputs.doNotCacheIf("Chrome version is unknown; set CHROME_BIN to enable browser test caching") { !chromeVersion.isPresent }
    }
  }
}
