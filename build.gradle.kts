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
    exclude("**/node_modules/**", "**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/.vitepress/.temp/**")
  }

tasks.pnpmInstall {
  inputs.file("pnpm-workspace.yaml")
  inputs.files(workspacePackages)
  outputs.dirs(workspacePackages.elements.map { packages -> packages.map { it.asFile.resolveSibling("node_modules") } })
}

// Everything remark reads: the Markdown files that .gitignore doesn't exclude, remark's configuration files, and the
// lock file that pins remark's version. VitePress creates and deletes .vitepress/.temp during the site build.
val linkCheckSources =
  fileTree(layout.projectDirectory) {
    include(listOf("md", "markdown", "mdown", "mkdn", "mkd", "mdwn", "mkdown", "ron").map { "**/*.$it" })
    include("**/.remarkrc*", "**/.remarkignore", "**/package.json", ".gitignore", "pnpm-lock.yaml")
    exclude("**/node_modules/**", "**/build/**", "**/out/**", "**/.gradle/**", "**/.kotlin/**", "**/.vitepress/.temp/**")
  }

val checkLinks =
  tasks.register<PnpmTask>("checkLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks links to files, headings, and external URLs in all Markdown files."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "check-links")
    val ci = providers.environmentVariable("CI")
    // Locally, skip external URLs when offline. In CI, always check them.
    args.addAll(ci.map { emptyList<String>() }.orElse(listOf("--use", "remark-lint-no-dead-urls=skipOffline:true")))

    // Files that Markdown links to but that are neither Markdown nor configuration aren't inputs. Creating or deleting
    // one therefore doesn't rerun the check locally; CI always runs it.
    inputs.files(linkCheckSources).withPropertyName("linkCheckSources").withPathSensitivity(PathSensitivity.RELATIVE)
    val marker = layout.buildDirectory.file("checkLinks/success")
    outputs.file(marker).withPropertyName("marker")
    // External URLs can break without any input changing, so CI always runs the check.
    outputs.upToDateWhen { !ci.isPresent }
    outputs.cacheIf { !ci.isPresent }
    doLast { marker.get().asFile.writeText("") }
  }

tasks.check {
  dependsOn(checkLinks)
}
