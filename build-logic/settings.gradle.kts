rootProject.name = "build-logic"

dependencyResolutionManagement {
  versionCatalogs {
    create("buildLibs") {
      from(files("../gradle/buildLibs.versions.toml"))
    }
  }
}

include(
  "format",
  "multiplatform",
  "nodejs",
)
