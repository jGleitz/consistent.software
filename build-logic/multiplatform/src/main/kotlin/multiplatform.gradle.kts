plugins {
  kotlin("multiplatform")
  id("jvm-toolchain")
  id("multiplatform-targets-all")
  id("multiplatform-tests")
}

repositories {
  mavenCentral()
}

val compileAll =
  tasks.register("compileAll") {
    dependsOn(kotlin.targets.flatMap { target -> target.compilations.map { it.compileAllTaskName } })
  }

kotlin {
  compilerOptions {
    explicitApi()

    freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    freeCompilerArgs.add("-Xcollection-literals")
    freeCompilerArgs.add("-Xreturn-value-checker=full")
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}
