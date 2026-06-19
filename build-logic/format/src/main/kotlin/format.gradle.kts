import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessTask

plugins {
  base
  com.diffplug.spotless
  id("nodejs")
}

repositories {
  mavenCentral() // to download dependencies of the checkers
}

// TODO move into dependency catalog
val prettierVersion = "3.8.3"
// TODO move into dependency catalog
val klintVersion = "1.8.0"

val prettierRc = rootProject.projectDir.resolve(".prettierrc.yaml")

spotless {
  if (project.hasKotlin) {
    kotlin {
      from(projectDir.resolve("src")) {
        include("**/*.kt")
      }
      ktlint(klintVersion)
    }
  }

  kotlinGradle {
    from(projectDir) {
      include("**/*.gradle.kts")
    }
    ktlint(klintVersion)
  }

  format("markdown") {
    from(projectDir) {
      include("**/*.md")
    }
    csPrettier()
  }

  format("json") {
    from(projectDir) {
      include("**/*.json")
      include("**/*.json5")
    }
    csPrettier()
  }

  format("yaml") {
    from(projectDir) {
      include("**/*.yml")
    }
    csPrettier()
  }

  format("typescript") {
    from(projectDir) {
      include("**/*.ts")
      include("**/*.tsx")
    }
    csPrettier()
  }

  format("css") {
    from(projectDir) {
      include("**/*.css")
    }
    csPrettier()
  }

  format("properties") {
    from(projectDir) {
      include("**/*.properties")
      // this file is auto-updated by Renovate & Gradle, which have their own formatting rules
      exclude("gradle/wrapper/gradle-wrapper.properties")
    }
    csPrettier(
      // TODO move into dependency catalog
      "prettier-plugin-properties" to "0.3.1",
    ).config(mapOf("plugins" to listOf("prettier-plugin-properties")))
  }
}

val format by tasks.registering {
  dependsOn("spotlessApply")
}

val lint by tasks.registering {
  dependsOn("spotlessCheck")
}

tasks.named("check") {
  dependsOn(lint)
}

val Project.hasKotlin: Boolean get() = plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

fun ConfigurableFileTree.excludeSubprojects(): ConfigurableFileTree =
  apply {
    exclude(
      rootProject.subprojects
        .filter { it.projectDir.startsWith(this.dir) && it.projectDir != this.dir }
        .map { it.projectDir.relativeTo(this.dir).path + "/**" },
    )
  }

fun ConfigurableFileTree.excludeDefaultPatterns(): ConfigurableFileTree =
  apply {
    if (this.dir == projectDir) {
      excludeSubprojects()
      exclude(
        "**/node_modules/**",
        "**/.kotlin/**",
        "**/.gradle/**",
        "**/build/**",
        "**/.idea/**",
        "**/kotlin-js-store/**",
      )
    }
    exclude("yarn.lock", "pnpm-lock.yaml")
  }

fun FormatExtension.from(
  base: Any,
  config: Action<ConfigurableFileTree>,
) = target(
  fileTree(base) {
    excludeDefaultPatterns()
    config.execute(this)
  },
)

fun FormatExtension.csPrettier(vararg plugins: Pair<String, String>): FormatExtension.PrettierConfig =
  configurePrettier(
    prettier(
      mapOf(
        "prettier" to prettierVersion,
        *plugins,
      ),
    ),
  )

fun FormatExtension.csPrettier(): FormatExtension.PrettierConfig = configurePrettier(prettier(prettierVersion))

private fun Project.configurePrettier(prettier: FormatExtension.PrettierConfig) =
  prettier.configFile(prettierRc).nodeExecutable(nodeJs.nodeExecutable).npmExecutable(nodeJs.npmExecutable)

afterEvaluate {
  tasks.withType<SpotlessTask>().configureEach {
    if (stepsInternalRoundtrip.steps.any { it.name == "prettier-format" }) {
      dependsOn(nodeJs.npmSetupTask)
    }
  }
}
