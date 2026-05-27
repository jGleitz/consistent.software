plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:8.6.0")
}

tasks.validatePlugins {
  enableStricterValidation = true
}
