import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
  id("nodejs")
  id("multiplatform")
}

// Retain the locked tooling versions while adapting workspace entries to this smaller fixture.
extensions.configure<NpmExtension> {
  packageLockMismatchReport.set(LockFileMismatchReport.NONE)
  packageLockAutoReplace.set(true)
}

kotlin {
  js {
    browser {
      testTask {
        providers.environmentVariable("TEST_TASK_CHROME_BIN").orNull?.let { environment("CHROME_BIN", it) }
        // Only the probe receives this preload; npm and other Node processes run normally.
        val preload =
          layout.projectDirectory
            .file("mock-browser-process.cjs")
            .asFile.absolutePath
            .replace("\\", "/")
        environment("NODE_OPTIONS", "--require=\"$preload\"")

        // Keep the real task and the convention plugin's inputs, cache rules and npm dependency.
        // The main build already exercises Karma's actual browser-test execution.
        val result =
          layout.buildDirectory
            .file("mock-browser-result.txt")
            .get()
            .asFile
        val executions = layout.projectDirectory.file("browser-executions.txt").asFile
        outputs.file(result)
        actions.clear()
        doLast {
          result.parentFile.mkdirs()
          result.writeText("mock browser result")
          executions.appendText("executed\n")
        }
      }
    }
  }
}
