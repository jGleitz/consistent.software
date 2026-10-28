@file:OptIn(ExperimentalJsTestDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
  id("com.google.devtools.ksp")
  id("io.kotest")
  id("nodejs")
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
  // A local UP-TO-DATE result would suppress the failure events, too.
  outputs.upToDateWhen { !ignoreFailures }
}

tasks.withType<KotlinTest>().configureEach {
  inputs.property("ignoreRunFailures", provider { ignoreRunFailures })
  outputs.doNotCacheIf("Test runner failures would be cached as passing") { ignoreRunFailures }
  outputs.upToDateWhen { !ignoreRunFailures }
}

// KGP disables caching for JS tests without giving a reason (see KT-78586). We enable it and declare the inputs that KGP leaves
// out. Node modules are covered: the tasks already track the installed package-lock.json, which also pins Playwright and so the
// browser it downloads.
tasks.withType<KotlinJsTest>().configureEach {
  val testTask = this
  // KotlinJsTest stores this action, so it must not capture the project through the `nodeJs` or `providers` accessors.
  val nodeJs = nodeJs
  val providers = providers
  onTestFrameworkSet {
    // The test framework determines which Node executable the test launches.
    nodeJs.declareAsInput(testTask, executable)
    // A shorter timeout can make passing tests fail.
    val framework = this
    if (framework is KotlinMocha) testTask.inputs.property("mochaTimeout", providers.provider { framework.timeout })
  }

  outputs.cacheIf { true }
}

kotlin {
  targets.withType<KotlinJsIrTarget>().configureEach {
    whenBrowserConfigured {
      test { chromium() }

      // KGP declares only the test entry file as the bundle's input. Without the modules it imports, the bundle would stay
      // up to date after a change to the code under test, and the browser tests would run against the old code.
      // KGP bundles only the default test run, and that run's input file is the entry file of the bundle.
      val testModules =
        testRuns
          .named("test")
          .flatMap { it.executionTask }
          .flatMap { it.inputFileProperty }
          .map { it.asFile.parentFile }
      tasks.named("prepareWebpackBundleForKotlinJsTests") {
        inputs.files(testModules).withPropertyName("testModules")
        nodeJs.declareAsInput(this, project.kotlinNodeJsEnvSpec.executable)
      }

      // KGP declares the host-wide Playwright browser directory as an input of the browser tests, while each project's install
      // task writes to it under a lock (see KT-87599). On Windows, a test cannot fingerprint the locked file while another
      // project installs. So that the fingerprint is stable, browser tests run only after every installation.
      // TODO Remove this workaround once KGP no longer fingerprints the browser directory while installing into it (KT-87599).
      testRuns.named("test") {
        executionTask.configure {
          mustRunAfter(rootProject.allprojects.map { it.tasks.named { it == "kotlinInstallPlaywrightBrowsers" } })
        }
      }
    }
  }
}
