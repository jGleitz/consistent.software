@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

kotlin {
  jvm()
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
