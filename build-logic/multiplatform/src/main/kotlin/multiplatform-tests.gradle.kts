import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.PathSensitivity.RELATIVE
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
  val testEnvironment = providers.provider { environment.toMap() }
  val nodeVersion =
    rootProject.the<NodeJsExtension>().nodeExecutable.flatMap { node ->
      providers
        .exec { commandLine(node, "--version") }
        .standardOutput.asText
        .map { it.trim() }
    }
  inputs.property("nodeJsVersion", nodeVersion)
  inputs.files("karma.config.d", "webpack.config.d").withPropertyName("jsConfigDirectories").withPathSensitivity(RELATIVE)
  onTestFrameworkSet {
    if (this is KotlinKarma) {
      // ValueSource providers do not propagate task dependencies from their parameters.
      // Install npm before input snapshotting loads the launcher; do not query the provider during configuration.
      dependsOn(":kotlinNpmInstall")
      val karma = this
      val windows = providers.systemProperty("os.name").get().startsWith("Windows")
      val chromeIdentity =
        providers.of(KarmaChromeIdentity::class) {
          parameters.nodeExecutable.set(karma.executable)
          parameters.workingDirectory.set(karma.workingDir)
          parameters.environment.set(testEnvironment)
          parameters.resolveExecutable.set(windows)
        }
      // CI must detect a broken probe instead of silently running browser tests without caching.
      val requireChromeIdentity = providers.gradleProperty("requireChromeIdentity").map(String::toBoolean).getOrElse(false)
      if (windows) {
        // Windows Chrome's --version is not a console interface. Fingerprint the executable instead of invoking PowerShell.
        // Like the former executable version-resource check, this assumes runtime updates also change the executable.
        inputs
          .file(chromeIdentity)
          .withPropertyName("chromeExecutable")
          .withPathSensitivity(NONE)
          .optional(!requireChromeIdentity)
      } else {
        // On Unix the selected executable may be an unchanged shell wrapper around an updated browser.
        inputs.property("chromeVersion", chromeIdentity).optional(!requireChromeIdentity)
      }
      outputs.doNotCacheIf("Chrome could not be identified") { !chromeIdentity.isPresent }
    }
  }
}
