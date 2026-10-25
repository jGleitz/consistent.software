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
        val browserHome = providers.environmentVariable("TEST_BROWSER_HOME").get()
        when {
          System.getProperty("os.name").startsWith("Windows") -> {
            listOf("LOCALAPPDATA", "PROGRAMFILES", "PROGRAMFILES(X86)", "ProgramW6432").forEach { environment(it, browserHome) }
          }

          System.getProperty("os.name").startsWith("Mac") -> {
            environment("HOME", browserHome)
          }
        }
      }
    }
  }
}
