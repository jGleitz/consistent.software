pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.develocity") version "4.4.0"
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
