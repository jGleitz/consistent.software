import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding

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
  // Matches `end_of_line` in .editorconfig. The default policy reads Git attributes through JGit, which reads `PATH`
  // during configuration, so any change to `PATH` (for example IDE versus terminal) would discard the configuration cache.
  lineEndings = LineEnding.UNIX

  kotlinGradle {
    from(projectDir) {
      include("**/*.gradle.kts")
    }
    ktlint(klintVersion)
  }

  prettierFormat("markdown") {
    from(projectDir) {
      include("**/*.md")
    }
    csPrettier()
    toggleOffOn("@formatter:off", "@formatter:on")
  }

  prettierFormat("json") {
    from(projectDir) {
      include("**/*.json")
      include("**/*.json5")
    }
    csPrettier()
  }

  prettierFormat("yaml") {
    from(projectDir) {
      include("**/*.yml")
    }
    csPrettier()
  }

  prettierFormat("typescript") {
    from(projectDir) {
      include("**/*.ts")
      include("**/*.tsx")
    }
    csPrettier()
  }

  prettierFormat("css") {
    from(projectDir) {
      include("**/*.css")
    }
    csPrettier()
  }

  prettierFormat("properties") {
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

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
  spotless {
    kotlin {
      from(projectDir.resolve("src")) {
        include("**/*.kt")
      }
      ktlint(klintVersion)
    }
  }
}

val format =
  tasks.register("format") {
    dependsOn("spotlessApply")
  }

val lint =
  tasks.register("lint") {
    dependsOn("spotlessCheck")
  }

tasks.named("check") {
  dependsOn(lint)
}

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
        "**/.omo/**",
        "**/.codegraph/**",
        // VitePress writes this directory during its build and deletes it afterwards. Its location is not configurable.
        "**/.vitepress/.temp/**",
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

fun SpotlessExtension.prettierFormat(
  name: String,
  config: Action<FormatExtension>,
) {
  format(name, config)
}
