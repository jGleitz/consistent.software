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
    include("mise.toml", "mise.lock", "gradle.properties", "gradle/testLibs.versions.toml")
    into("fixture")
  }
}

abstract class TestRuntime : CommandLineArgumentProvider {
  @get:Internal
  abstract val gradleUserHome: DirectoryProperty

  @get:Internal
  abstract val nodeExecutable: Property<String>

  override fun asArguments() =
    listOf("-Dtest.gradleUserHome=${gradleUserHome.get().asFile.absolutePath}", "-Dtest.nodeExecutable=${nodeExecutable.get()}")
}

val testNode =
  providers
    .exec {
      commandLine("mise", "which", "node")
      workingDir(rootProject.projectDir.parentFile)
    }.standardOutput.asText
    .map { it.trim() }

tasks.test {
  val providers = project.providers
  inputs.property(
    "nodeRuntime",
    testNode.flatMap { node ->
      providers.exec { commandLine(node, "-p", "JSON.stringify([process.version,process.platform,process.arch])") }.standardOutput.asText
    },
  )
  jvmArgumentProviders.add(
    objects.newInstance<TestRuntime>().apply {
      gradleUserHome.fileValue(gradle.gradleUserHomeDir)
      nodeExecutable.set(testNode)
    },
  )
}
