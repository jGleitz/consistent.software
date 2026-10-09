plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation(buildLibs.plugin.dokka)
}

tasks.validatePlugins {
  enableStricterValidation = true
}
