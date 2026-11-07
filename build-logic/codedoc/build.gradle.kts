plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(buildLibs.plugin.dokka)
}

tasks.validatePlugins {
  enableStricterValidation = true
}
