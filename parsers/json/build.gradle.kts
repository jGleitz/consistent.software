@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.util.DependencyDirectories.localKonanDir

plugins {
  id("format")
  id("multiplatform")
  id("nodejs")
  id("io.github.lemcoder.konanplugin") version "1.1.0"
  id("io.github.tree-sitter.ktreesitter-plugin") version "0.25.1"
}

dependencies {
  jvmMainApi(libs.ktreesitter.runtime)
  nativeMainApi(libs.ktreesitter.runtime)
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

val generateGrammarHeader =
  tasks.register("generateGrammarHeader") {
    inputs.property("grammarName", grammar.grammarName)
    outputs.file(
      layout.buildDirectory.dir("generated/src/c").zip(grammar.grammarName) { headerDir, grammarName ->
        headerDir.file("tree-sitter-$grammarName.h")
      },
    )
    mustRunAfter(tasks.generateGrammarFiles)

    doLast {
      val grammarName = inputs.properties["grammarName"] as String
      outputs.files.singleFile.writeText(
        """
        |#ifndef TREE_SITTER_JSON_H_
        |#define TREE_SITTER_JSON_H_
        |typedef struct TSLanguage TSLanguage;
        |#ifdef __cplusplus
        |extern "C" {
        |#endif
        |const TSLanguage *tree_sitter_$grammarName(void);
        |#ifdef __cplusplus
        |}
        |#endif
        |#endif // TREE_SITTER_JSON_H_
        |
        """.trimMargin(),
      )
    }
  }

konanConfig {
  targets.addAll(kotlin.targets.withType<KotlinNativeTarget>().map { it.konanTarget.name })
  sourceDir = grammar.baseDir.map { it.resolve("src").path }
  headerDir = generateGrammarHeader.map { it.outputs.files.singleFile.parent }
  libName = grammar.grammarName.map { "tree-sitter-$it" }
  outputDir = layout.buildDirectory.dir("konan-lib").map { it.asFile.path }
  konanPath =
    kotlin.compilerVersion.map { kotlinVersion ->
      localKonanDir.listFiles()?.first { it.name.contains(kotlinVersion) }?.path
    }
}

kotlin {
  for (sourceSetName in listOf("commonMain", "jvmMain", "nativeMain")) {
    sourceSets.named(sourceSetName) {
      generatedKotlin.srcDir(tasks.generateGrammarFiles.flatMap { it.generatedSrc.dir("$sourceSetName/kotlin") })
    }
  }

  targets.withType<KotlinNativeTarget>().configureEach {
    val target = this
    compilations.named("main").configure {
      cinterops {
        create(grammar.interopName.get()) {
          definitionFile = tasks.generateGrammarFiles.flatMap { it.generatedSrc.file("nativeInterop/${it.interopName}.def") }
          includeDirs.allHeaders(generateGrammarHeader.map { it.outputs.files.singleFile.parent })
          extraOpts("-libraryPath", konanConfig.outputDir.get() + "/" + target.konanTarget.name)
        }
      }
    }
  }
  compilerOptions {
    explicitApi = Disabled
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}

tasks.withType<CInteropProcess>().configureEach {
  dependsOn(generateGrammarHeader, tasks.runKonanClang)
}

val pnpmInstall = nodeJs.registerPnpmInstall()

tasks.generateGrammarFiles {
  dependsOn(pnpmInstall)
}
