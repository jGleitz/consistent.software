import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport.NONE
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
  id("nodejs")
  id("multiplatform")
}

// This smaller fixture has different workspace entries; keep the dependency versions from the actual plugin.
extensions.configure<NpmExtension> {
  packageLockMismatchReport.set(NONE)
  packageLockAutoReplace.set(true)
}
