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

// pnpm installs the whole workspace. Matching every package.json is broader than the workspace, but needs no upkeep.
val workspacePackages =
  fileTree(layout.projectDirectory) {
    include("**/package.json")
    exclude("**/node_modules/**", "**/build/**", "**/.gradle/**", "**/.kotlin/**")
  }

tasks.pnpmInstall {
  inputs.file("pnpm-workspace.yaml")
  inputs.files(workspacePackages)
  outputs.dirs(workspacePackages.elements.map { packages -> packages.map { it.asFile.resolveSibling("node_modules") } })
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
