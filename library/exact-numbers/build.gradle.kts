plugins {
  id("codestyle")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  jvmMainApi(libs.apache.commons.fraction)

  jsMainApi(npm(libs.fraction.js))

  commonTestImplementation(testLibs.kotest.property)
}
