plugins {
  id("format")
  id("multiplatform")
  id("codedoc")
}

dependencies {
  commonMainImplementation(project(":library:exact-numbers"))
  commonMainImplementation(project(":library:monospace-layout"))

  commonTestImplementation(testLibs.kotest.property)
  commonTestImplementation(testLibs.kotest.property.extraArbs)
}
