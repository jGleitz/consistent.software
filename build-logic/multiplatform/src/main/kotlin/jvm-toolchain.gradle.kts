plugins {
  kotlin("multiplatform")
}

val jvmMajorVersion =
  Regex("""(?m)^java=(\d+)""")
    .find(rootProject.projectDir.resolve(".sdkmanrc").readText())
    ?.groupValues
    ?.get(1)
    ?.toInt()
    ?: error("No java=<version> entry found in .sdkmanrc")

kotlin {
  jvmToolchain(jvmMajorVersion)
}
