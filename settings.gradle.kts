rootProject.name = "consistent.software"

pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.develocity") version "4.6.1"
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
  ":library:exact-numbers",
  ":library:monospace-layout",
  ":data",
)

develocity {
  buildScan {
    publishing.onlyIf { false }
  }
}
