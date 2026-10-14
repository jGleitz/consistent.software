import com.github.gradle.node.pnpm.task.PnpmTask

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

val checkLinks =
  tasks.register<PnpmTask>("checkLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks links to files, headings, and external URLs in all Markdown files."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "check-links")
  }

tasks.check {
  dependsOn(checkLinks)
}
