pluginManagement {
  includeBuild("build-logic")
}

plugins {
    id("com.gradle.develocity") version "4.3.2" apply false
}

include(
    ":documentation",
    ":model"
)

val requestedBuildScan = providers.gradleProperty("scan")
    .map { it == "true" || it == ""}
    .orElse(false)

if (requestedBuildScan.get()) {
    plugins.apply("com.gradle.develocity")
    develocity {
        buildScan {
            termsOfUseUrl = "https://gradle.com/terms-of-service"
            termsOfUseAgree = "yes"
        }
    }
}
