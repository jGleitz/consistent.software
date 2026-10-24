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
    description = "Checks links to files and headings in all Markdown files."
    dependsOn(tasks.pnpmInstall)
    args = listOf("run", "check-links")
    val ci = providers.environmentVariable("CI")

    // Files that Markdown links to but that are neither Markdown nor configuration aren't inputs. Creating or deleting
    // one therefore doesn't rerun the check locally; CI always runs it.
    inputs.files(linkCheckSources).withPropertyName("linkCheckSources").withPathSensitivity(PathSensitivity.RELATIVE)
    val marker = layout.buildDirectory.file("checkLinks/success")
    outputs.file(marker).withPropertyName("marker")
    outputs.upToDateWhen { !ci.isPresent }
    outputs.cacheIf { !ci.isPresent }
    doLast { marker.get().asFile.writeText("") }
  }

// The site sources are left out because checkSiteLinks checks the URLs of the built site.
val checkExternalLinks =
  tasks.register<Exec>("checkExternalLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks external URLs in all Markdown files outside the documentation site."
    // External URLs can break without any input changing, and we don't want local builds to depend on the network.
    val ci = providers.environmentVariable("CI")
    onlyIf("external URLs are only checked in CI") { ci.isPresent }
    commandLine(
      "mise",
      "exec",
      "--",
      "lychee",
      "--config",
      "lychee.toml",
      "--extensions",
      "md",
      "--scheme",
      "https",
      "--scheme",
      "http",
      "--include-fragments",
      "--exclude-path",
      "(^|/)documentation/src/main/markdown/",
      ".",
    )
  }

tasks.check {
  dependsOn(checkLinks, checkExternalLinks)
}
