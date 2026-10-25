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
  testImplementation(gradleTestKit())
  testImplementation(buildLibs.test.junit)
}

tasks.validatePlugins {
  enableStricterValidation = true
}

tasks.processTestResources {
  from(rootProject.projectDir.parentFile) {
    include(
      "mise.toml",
      "mise.lock",
      ".sdkmanrc",
      "gradle.properties",
      "gradle/testLibs.versions.toml",
      "gradle/kotlin-js-store/package-lock.json",
    )
    into("fixture")
  }
}

abstract class TestKitArguments : CommandLineArgumentProvider {
  @get:Internal
  abstract val gradleUserHome: DirectoryProperty

  override fun asArguments() = listOf("-Dtest.gradleUserHome=${gradleUserHome.get().asFile.absolutePath}")
}

tasks.test {
  inputs.property("operatingSystem", providers.systemProperty("os.name"))
  failFast = true
  testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  // This location controls download reuse, not test semantics; keep it out of the task's cache key.
  jvmArgumentProviders.add(objects.newInstance<TestKitArguments>().apply { gradleUserHome.fileValue(gradle.gradleUserHomeDir) })
}
