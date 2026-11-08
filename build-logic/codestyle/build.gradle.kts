plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(buildLibs.plugin.spotless)
  implementation(project(":nodejs"))
  implementation(project(":conventions"))
}

tasks.validatePlugins {
  enableStricterValidation = true
}
