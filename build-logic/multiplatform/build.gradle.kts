plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation(buildLibs.plugin.kotlin.multiplatform)
  implementation(buildLibs.plugin.kotest)
  implementation(buildLibs.plugin.ksp)
}

tasks.validatePlugins {
  enableStricterValidation = true
}
