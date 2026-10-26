rootProject.name = "managed-chrome-test"

dependencyResolutionManagement {
  versionCatalogs {
    create("testLibs") { from(files("gradle/testLibs.versions.toml")) }
  }
}
