plugins {
  kotlin("multiplatform")
}

val jvmMajorVersion =
  Regex("""(?m)^java\s*=\s*"temurin-(\d+)""")
    .find(providers.fileContents(rootProject.layout.projectDirectory.file("mise.toml")).asText.get())
    ?.groupValues
    ?.get(1)
    ?.toInt()
    ?: error("No java = \"temurin-<version>\" entry found in mise.toml")

kotlin {
  jvmToolchain(jvmMajorVersion)
}
