rootProject.name = "consistent.software"

pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.develocity") version "4.4.2"
}

dependencyResolutionManagement {
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
  ":json",
  ":model",
  ":parsers:json",
)

develocity {
  buildScan {
    publishing.onlyIf { false }
  }
}
