import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import software.consistent.build.conventions.buildLibs
import software.consistent.build.conventions.get

plugins {
  base
  com.diffplug.spotless
  id("nodejs")
}

val prettierRc = rootProject.projectDir.resolve(".prettierrc.yaml")

spotless {
  // Matches `end_of_line` in .editorconfig. The default policy reads Git attributes through JGit,
  // which reads `PATH` during configuration, so any change to `PATH` (for example IDE versus
  // terminal) would discard the configuration cache.
  lineEndings = LineEnding.UNIX

  kotlin {
    from(projectDir.resolve("src")) {
      include("**/*.kt")
    }
    ktfmt(buildLibs["ktfmt"].get().version)
  }

  kotlinGradle {
    from(projectDir) {
      include("**/*.gradle.kts")
    }
    ktfmt(buildLibs["ktfmt"].get().version)
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
      include("**/*.yaml")
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
            buildLibs["prettier-plugin-properties"],
        )
        .config(mapOf("plugins" to listOf("prettier-plugin-properties")))
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

val Project.hasKotlin: Boolean
  get() = plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

fun ConfigurableFileTree.excludeSubprojects(): ConfigurableFileTree = apply {
  exclude(
      rootProject.subprojects
          .filter { it.projectDir.startsWith(this.dir) && it.projectDir != this.dir }
          .map { it.projectDir.relativeTo(this.dir).path + "/**" },
  )
}

fun ConfigurableFileTree.excludeDefaultPatterns(): ConfigurableFileTree = apply {
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
        // VitePress writes this directory during its build and deletes it afterwards. Its location
        // is not configurable.
        "**/.vitepress/.temp/**",
    )
  }
  exclude("yarn.lock", "pnpm-lock.yaml")
}

fun FormatExtension.from(
    base: Any,
    config: Action<ConfigurableFileTree>,
) =
    target(
        fileTree(base) {
          excludeDefaultPatterns()
          config.execute(this)
        },
    )

fun FormatExtension.csPrettier(
    vararg plugins: Provider<MinimalExternalModuleDependency>,
): FormatExtension.PrettierConfig {
  val devDependencies =
      listOf(buildLibs["prettier"], *plugins).map { it.get() }.map { it.name to it.version }.toMap()
  return configurePrettier(prettier(devDependencies))
}

fun FormatExtension.csPrettier(): FormatExtension.PrettierConfig =
    configurePrettier(prettier(buildLibs["prettier"].get().version))

private fun Project.configurePrettier(prettier: FormatExtension.PrettierConfig) =
    prettier
        .configFile(prettierRc)
        .nodeExecutable(nodeJs.nodeExecutable)
        .npmExecutable(nodeJs.npmExecutable)

fun SpotlessExtension.prettierFormat(
    name: String,
    config: Action<FormatExtension>,
) {
  format(name, config)
}
