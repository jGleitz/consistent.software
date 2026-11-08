plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(project(":conventions"))
  implementation(project(":nodejs"))
  implementation(buildLibs.plugin.kotlin.multiplatform)
  implementation(buildLibs.plugin.kotest)
  implementation(buildLibs.plugin.kover)
  implementation(buildLibs.plugin.ksp)
}

tasks.validatePlugins {
  enableStricterValidation = true
}
