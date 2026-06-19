plugins {
  id("com.google.devtools.ksp")
  id("io.kotest")
}

kotlin {
  sourceSets {
    named("nonNativeTest") {
      dependencies {
        implementation(testLibs["kotest-framework"])
        implementation(testLibs["atrium-fluent"])
      }
    }
    jvmTest {
      dependencies {
        runtimeOnly(testLibs["kotest-runner-junit5"])
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
