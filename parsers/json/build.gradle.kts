@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
  id("format")
  id("multiplatform")
  id("nodejs")
  id("io.github.tree-sitter.ktreesitter-plugin") version "0.25.0"
}

grammar {
  baseDir = projectDir.resolve("node_modules/tree-sitter-json")

  grammarName = "json"
  className = "TreeSitterJson"
  packageName = "io.github.treesitter.language.json"
  interopName = grammarName.map { "$it-grammar" }
  files =
    arrayOf(
      projectDir.resolve("node_modules/tree-sitter-json/src/grammar.c"),
    )
}

kotlin {
  for (sourceSetName in listOf("commonMain", "jvmMain", "nativeMain")) {
    sourceSets.named(sourceSetName) {
      generatedKotlin.srcDir(tasks.generateGrammarFiles.flatMap { it.generatedSrc.dir("$sourceSetName/kotlin") })
    }
  }

  targets.withType<KotlinNativeTarget>().configureEach {
    compilations.named("main").configure {
      cinterops {
        create(grammar.interopName.get()) {
          packageName = grammar.packageName.get()
        }
      }
    }
  }
  compilerOptions {
    explicitApi = Disabled
  }
}

val pnpmInstall = nodeJs.registerPnpmInstall()

tasks.generateGrammarFiles {
  dependsOn(pnpmInstall)
}
