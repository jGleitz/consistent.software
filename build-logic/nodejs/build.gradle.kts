plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  api(buildLibs.plugin.nodejs)
}

tasks.validatePlugins {
  enableStricterValidation = true
}
