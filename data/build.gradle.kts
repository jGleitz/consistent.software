plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonMainImplementation(project(":library:exact-numbers"))
  commonMainImplementation(project(":library:monospace-layout"))
  commonMainImplementation(project(":library:collections"))
  commonMainImplementation(libs.androidx.collection)

  commonTestImplementation(testLibs.kotest.property)
  commonTestImplementation(testLibs.kotest.property.extraArbs)
}
