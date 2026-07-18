@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

val jvmMajorVersion =
  Regex("""(?m)^java=(\d+)""")
    .find(rootProject.projectDir.resolve(".sdkmanrc").readText())
    ?.groupValues
    ?.get(1)
    ?.toInt()
    ?: error("No java=<version> entry found in .sdkmanrc")

kotlin {
  jvm()
  jvmToolchain(jvmMajorVersion)
  js {
    browser()
    nodejs()
    binaries.library()
    useEsModules()
    generateTypeScriptDefinitions()
  }
  linuxX64()

  compilerOptions {
    explicitApi()
    optIn.add("kotlin.js.ExperimentalJsExport")
  }

  applyDefaultHierarchyTemplate {
    common {
      group("nonNative") {
        withJvm()
        withJs()
      }
    }
  }
}
