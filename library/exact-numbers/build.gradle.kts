plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonTestImplementation(testLibs.kotest.property)
}

kotlin {
  sourceSets {
    jvmMain.dependencies {
      api("org.apache.commons:commons-numbers-fraction:1.3")
    }
    jsMain.dependencies {
      api(npm("fraction.js", "5.3.4"))
    }
  }
}
