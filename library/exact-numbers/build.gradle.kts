import software.consistent.build.kmp.npm

plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  jvmMainApi(libs.apache.commons.fraction)

  jsMainApi(npm(libs.fraction.js))

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
