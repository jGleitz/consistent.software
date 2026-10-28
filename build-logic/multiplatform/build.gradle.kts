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
}

tasks.validatePlugins {
  enableStricterValidation = true
}
