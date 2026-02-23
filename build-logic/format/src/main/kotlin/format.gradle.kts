import com.diffplug.gradle.spotless.FormatExtension

plugins {
  id("com.diffplug.spotless")
}

repositories {
  mavenCentral() // to download dependencies of the checkers
}

val prettierRc = rootProject.projectDir.resolve(".prettierrc.yaml")

spotless {
  if (project.hasKotlin) {
    kotlin {
      from(projectDir.resolve("src")) {
        include("**/*.kt")
      }
      ktlint("1.8.0")
    }
  }

  kotlinGradle {
    from(projectDir) {
      include("**/*.gradle.kts")
    }
    ktlint("1.8.0")
  }

  format("markdown") {
    from(projectDir) {
      include("**/*.md")
    }
    prettier("3.8.1").configFile(prettierRc)
  }

  format("json") {
    from(projectDir) {
      include("**/*.json")
      include("**/*.json5")
    }
    prettier("3.8.1").configFile(prettierRc)
  }

  format("yaml") {
    from(projectDir) {
      include("**/*.yml")
    }
    prettier("3.8.1").configFile(prettierRc)
  }

  format("typescript") {
    from(projectDir) {
      include("**/*.ts")
      include("**/*.tsx")
    }
    prettier("3.8.1").configFile(prettierRc)
  }

  format("css") {
    from(projectDir) {
      include("**/*.css")
    }
    prettier("3.8.1").configFile(prettierRc)
  }

  format("properties") {
    from(projectDir) {
      include("**/*.properties")
    }
    prettier(
      mapOf(
        "prettier" to "3.8.1",
        "prettier-plugin-properties" to "0.3.1",
      ),
    ).configFile(prettierRc)
      .config(mapOf("plugins" to listOf("prettier-plugin-properties")))
  }
}

val format by tasks.registering {
  dependsOn("spotlessApply")
}

val lint by tasks.registering {
  dependsOn("spotlessCheck")
}

tasks.named("check").configure {
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
