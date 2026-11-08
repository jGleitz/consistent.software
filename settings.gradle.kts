rootProject.name = "consistent.software"

// Gradle hides these files from all file trees by default, so the format checks could not see them
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitattributes")

@Suppress("UnstableApiUsage")
gradle.lifecycle.beforeProject {
  dependencyLocking { lockAllConfigurations() }
}

pluginManagement {
  includeBuild("build-logic")
  repositories {
    // See the explanation in dependencyResolutionManagement below. This includes plugin markers:
    // those on Maven Central declare the same plugin as the Portal’s, and the Portal only redirects
    // to Central for some of them, which can fail with a 429, too. Markers that only the Portal has
    // are missing from the mirror, so Gradle falls back to the Portal for them.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Google Maven Central Mirror"
    }
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "4.6.1"
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  @Suppress("UnstableApiUsage")
  repositories {
    // Only Google’s Maven repository has androidx. We ask it first, so that Gradle doesn’t look for
    // androidx in the repositories below, where it is missing anyway, and where Maven Central can
    // fail the lookup with ‘429 Too Many Requests’. The filter keeps Gradle from asking it for
    // anything else.
    google {
      content { includeGroupByRegex("androidx\\..*") }
    }
    // Maven Central answers shared CI machines, such as those of Renovate and GitHub Actions, with
    // ‘429 Too Many Requests’ at times, which fails the whole build. So we first ask Google’s
    // mirror of Maven Central, which serves the same artifacts. Gradle falls back to the next
    // repository only if an artifact is missing, for example a release that hasn’t reached the
    // mirror yet; build-logic/settings.gradle.kts declares the mirror, too.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Google Maven Central Mirror"
    }
    mavenCentral()
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
    // Publishing a scan with `--scan` requires agreeing to gradle.com’s terms of service. Whoever
    // sets AGREE_TO_GRADLE_COM_TERMS_OF_SERVICE=yes agrees to them, for example our CI and the
    // cloud environment of AI agents.
    if (providers.environmentVariable("AGREE_TO_GRADLE_COM_TERMS_OF_SERVICE").orNull == "yes") {
      termsOfUseUrl = "https://gradle.com/terms-of-service"
      termsOfUseAgree = "yes"
    }
  }
}
