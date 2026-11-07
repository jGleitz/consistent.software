@file:Suppress("UnstableApiUsage")

rootProject.name = "consistent.software"

gradle.lifecycle.beforeProject {
  dependencyLocking { lockAllConfigurations() }
}

pluginManagement {
  includeBuild("build-logic")
  repositories {
    // See the explanation in dependencyResolutionManagement below
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Google Maven Central Mirror"
      // The Gradle Plugin Portal serves other plugin marker POMs than Maven Central does under the same coordinates, and
      // gradle/verification-metadata.xml has the checksums of the Portal’s
      content { excludeModuleByRegex(".*", ".*[.]gradle[.]plugin") }
    }
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "4.6.1"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    /* Maven Central answers shared CI machines, such as those of Renovate and GitHub Actions, with ‘429 Too Many Requests’ at
      times, which fails the whole build. So we first ask Google’s mirror of Maven Central, which serves the same artifacts.
      Gradle falls back to the next repository only if an artifact is missing, for example a release that hasn’t reached the
      mirror yet; build-logic/settings.gradle.kts declares the mirror, too. */
    maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "Google Maven Central Mirror" }
    mavenCentral()
    google {
      content { includeGroupByRegex("androidx\\..*") }
    }
  }
  versionCatalogs {
    create("testLibs") {
      from(files("gradle/testLibs.versions.toml"))
    }
    create("buildLibs") {
      from(files("gradle/buildLibs.versions.toml"))
    }
  }
}

include(
  ":documentation",
  ":library:collections",
  ":library:exact-numbers",
  ":library:monospace-layout",
  ":data",
)

develocity {
  buildScan {
    publishing.onlyIf { false }
    if (providers.environmentVariable("CI").orNull == "true") {
      termsOfUseUrl = "https://gradle.com/terms-of-service"
      termsOfUseAgree = "yes"
    }
  }
}
