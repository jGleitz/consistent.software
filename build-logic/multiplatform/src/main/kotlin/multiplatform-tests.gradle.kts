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
  // npm must be installed before input snapshotting loads the launcher. Do not query the version provider during configuration.
  dependsOn(rootProject.tasks.named("kotlinNpmInstall"))
  val providers = project.providers
  val testEnvironment = providers.provider { environment.toMap() }
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
      val karma = this
      val chromeVersion =
        providers.of(KarmaChromeVersion::class) {
          parameters.nodeExecutable.set(karma.executable)
          parameters.workingDirectory.set(karma.workingDir)
          parameters.environment.set(testEnvironment)
        }
      inputs.property("chromeVersion", chromeVersion).optional(true)
      outputs.doNotCacheIf("Chrome version could not be identified") { !chromeVersion.isPresent }
    }
  }
}
