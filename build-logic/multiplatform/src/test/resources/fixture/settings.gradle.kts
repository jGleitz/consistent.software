rootProject.name = "browser-cache-test"

dependencyResolutionManagement {
  versionCatalogs {
    create("testLibs") { from(files("gradle/testLibs.versions.toml")) }
  }
}

buildCache {
  local { directory = file("cache") }
}
