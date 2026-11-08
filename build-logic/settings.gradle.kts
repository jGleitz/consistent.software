@file:Suppress("UnstableApiUsage")

rootProject.name = "build-logic"

gradle.lifecycle.beforeProject {
  dependencyLocking { lockAllConfigurations() }
}

pluginManagement {
  repositories {
    // We ask Google’s mirror of Maven Central first; ../settings.gradle.kts explains why
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Google Maven Central Mirror"
    }
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    // The same as in pluginManagement, because the build-logic projects depend on plugin markers,
    // too
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Google Maven Central Mirror"
    }
    gradlePluginPortal()
  }
  versionCatalogs {
    create("buildLibs") {
      from(files("../gradle/buildLibs.versions.toml"))
    }
  }
}

include(
    "codedoc",
    "codestyle",
    "conventions",
    "multiplatform",
    "nodejs",
)
