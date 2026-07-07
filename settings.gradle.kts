rootProject.name = "consistent.software"

pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.develocity") version "4.5.0"
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
  ":model",
)

develocity {
  buildScan {
    publishing.onlyIf { false }
  }
}
