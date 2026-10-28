@file:OptIn(ExperimentalJsTestDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
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
  inputs.property("nodeJsVersion", rootProject.the<NodeJsExtension>().nodeVersion)
  inputs.property("operatingSystem", project.providers.systemProperty("os.name"))
  inputs.property("architecture", providers.systemProperty("os.arch"))

  outputs.cacheIf { true }
}

kotlin {
  targets.withType<KotlinJsIrTarget>().configureEach {
    whenBrowserConfigured {
      test { chromium() }

      // KGP declares only the test entry file as the bundle's input. Without the modules it imports, the bundle would stay
      // up to date after a change to the code under test, and the browser tests would run against the old code.
      val testModules =
        testRuns
          .named("test")
          .flatMap { it.executionTask }
          .flatMap { it.inputFileProperty }
          .map { it.asFile.parentFile }
      tasks.named("prepareWebpackBundleForKotlinJsTests") {
        inputs.files(testModules).withPropertyName("testModules")
        inputs.property("nodeJsVersion", rootProject.the<NodeJsExtension>().nodeVersion)
      }
    }
  }
}
