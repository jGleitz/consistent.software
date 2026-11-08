plugins {
  id("codestyle")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonTestImplementation(testLibs.kotest.property)
}
