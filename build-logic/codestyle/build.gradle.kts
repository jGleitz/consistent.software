plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(buildLibs.plugin.spotless)
  implementation(project(":nodejs"))
}

tasks.validatePlugins {
  enableStricterValidation = true
}
