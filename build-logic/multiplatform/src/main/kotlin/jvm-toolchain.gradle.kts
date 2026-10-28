plugins {
  kotlin("multiplatform")
}

kotlin {
  jvmToolchain(javaReleaseVersion.get().asInt())
}
