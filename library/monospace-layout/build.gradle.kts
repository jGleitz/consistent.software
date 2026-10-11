plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonMainImplementation(libs.unicodewidth)
}
