rootProject.name = "build-logic"

gradle.beforeProject {
  dependencyLocking { lockAllConfigurations() }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("buildLibs") {
      from(files("../gradle/buildLibs.versions.toml"))
    }
  }
}

include(
  "codedoc",
  "format",
  "multiplatform",
  "nodejs",
)
