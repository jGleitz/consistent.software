plugins {
  kotlin("multiplatform")
  id("multiplatform-targets")
  id("multiplatform-tests")
}

repositories {
  mavenCentral()
}

val compileAll =
  tasks.register("compileAll") {
    dependsOn(kotlin.targets.flatMap { target -> target.compilations.map { it.compileAllTaskName } })
  }
