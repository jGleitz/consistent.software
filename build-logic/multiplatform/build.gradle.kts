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
  testImplementation(buildLibs.test.junit)
}

tasks.validatePlugins {
  enableStricterValidation = true
}

tasks.test {
  inputs.property("operatingSystem", providers.systemProperty("os.name"))
}
