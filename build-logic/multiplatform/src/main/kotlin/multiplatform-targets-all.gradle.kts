@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

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

kotlin.js {
  nodejs {
    testTask {
      /* Mocha raises Error.stackTraceLimit to Infinity. Kotest runs tests about 700 frames deep, and Atrium reads the stack of every exception that
        toThrow catches, which source-map-support then maps frame by frame. Property tests that expect exceptions spent most of their time on this.
        We freeze the limit at 20 frames instead: enough to reach the test's own line when a property test fails. Raise it here to debug. */
      nodeJsArgs +=
        listOf(
          "--stack-trace-limit=20",
          "--import",
          "data:text/javascript,Object.defineProperty(Error,'stackTraceLimit',{writable:false})",
        )
    }
  }
}

rootProject.plugins.withType<NodeJsRootPlugin> {
  val installedNode = rootProject.the<NodeJsExtension>()
  rootProject.the<NodeJsEnvSpec>().apply {
    download.set(false)
    command.set(installedNode.nodeExecutable)
  }
  rootProject.the<NpmExtension>().command.set(installedNode.npmExecutable)
  rootProject.the<NpmExtension>().lockFileDirectory.set(rootProject.layout.projectDirectory.dir("gradle/kotlin-js-store"))
}
