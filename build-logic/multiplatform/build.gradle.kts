plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation(project(":nodejs"))
  implementation(buildLibs.plugin.kotlin.multiplatform)
  implementation(buildLibs.plugin.kotest)
  implementation(buildLibs.plugin.ksp)
  testImplementation(gradleTestKit())
  testImplementation(buildLibs.test.junit)
}

tasks.validatePlugins {
  enableStricterValidation = true
}

tasks.processTestResources {
  from(rootProject.projectDir.parentFile) {
    include(
      "mise.toml",
      "mise.lock",
      ".sdkmanrc",
      "gradle.properties",
      "gradle/testLibs.versions.toml",
      "gradle/kotlin-js-store/package-lock.json",
    )
    into("fixture")
  }
}

tasks.test {
  // Reuse the invoking build's dependency cache and network configuration for the real KMP fixture builds.
  systemProperty("test.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
}
