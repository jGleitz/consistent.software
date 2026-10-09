rootProject.name = "build-logic"

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
