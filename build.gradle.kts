plugins {
  id("nodejs")
  id("format")
  alias(buildLibs.plugins.kotlin.multiplatform) apply false
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL

  finalizedBy(tasks.spotlessPropertiesApply)
}

tasks.spotlessProperties {
  mustRunAfter(tasks.wrapper)
}
