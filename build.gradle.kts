plugins {
  id("nodejs")
  id("format")
  kotlin("multiplatform") version buildLibs.versions.kotlin.multiplatform apply false
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL

  finalizedBy(tasks.spotlessPropertiesApply)
}

tasks.spotlessProperties {
  mustRunAfter(tasks.wrapper)
}
