@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  kotlin("multiplatform")
}

/* Ideally, we would define the targets in the subprojects. This gives the subprojects more freedom and reduces configuration magic.
  However, we only get generated accessors (e.g. for dependency configurations) when the targets are applied during the plugins block.
  Hence, we apply the targets in a plugin.
 */
kotlin {
  jvm()
  js {
    browser()
    nodejs()
    binaries.library()
    useEsModules()
    generateTypeScriptDefinitions()
  }
  // TODO native support
  // linuxX64()

  compilerOptions {
    optIn.add("kotlin.js.ExperimentalJsExport")
  }
}

rootProject.plugins.withType<YarnPlugin> {
  rootProject.the<YarnRootExtension>().lockFileDirectoryProperty.set(rootProject.layout.projectDirectory.dir("gradle/kotlin-js-store"))
}
