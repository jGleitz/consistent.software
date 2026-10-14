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
    // Locally, skip external URLs when offline. In CI, always check them.
    args.addAll(
      providers
        .environmentVariable("CI")
        .map { emptyList<String>() }
        .orElse(listOf("--use", "remark-lint-no-dead-urls=skipOffline:true")),
    )
  }

tasks.check {
  dependsOn(checkLinks)
}
