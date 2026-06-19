plugins {
  kotlin("multiplatform")
  id("multiplatform-targets")
  id("multiplatform-tests")
}

repositories {
  mavenCentral()
}

val compileAll by tasks.registering {
  dependsOn(kotlin.targets.flatMap { target -> target.compilations.map { it.compileAllTaskName } })
}
