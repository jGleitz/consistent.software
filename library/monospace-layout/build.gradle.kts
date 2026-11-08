plugins {
  id("codestyle")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonMainImplementation(libs.unicodewidth)
}
