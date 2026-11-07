plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonTestImplementation(testLibs.kotest.property)
}
