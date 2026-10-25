plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

tasks.validatePlugins {
  enableStricterValidation = true
}
