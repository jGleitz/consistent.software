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

// The Markdown files and the configuration that decide lychee's result. VitePress creates and deletes .vitepress/.temp
// during the site build.
val linkCheckSources =
  fileTree(layout.projectDirectory) {
    include("**/*.md", "lychee.toml", "mise.toml", "mise.lock", ".gitignore")
    exclude("**/node_modules/**", "**/build/**", "**/out/**", "**/.gradle/**", "**/.kotlin/**", "**/.vitepress/.temp/**")
  }

// The site sources are left out: their links use VitePress's routing, which VitePress and checkSiteLinks check.
val checkLinks =
  tasks.register<Exec>("checkLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks links to files, headings, and external URLs in all Markdown files outside the documentation site."
    commandLine(
      "mise",
      "exec",
      "--",
      "lychee",
      "--config",
      "lychee.toml",
      "--extensions",
      "md",
      "--include-fragments",
      "--exclude-path",
      "(^|/)documentation/src/main/markdown/",
      ".",
    )
    val ci = providers.environmentVariable("CI")
    // Locally, skip external URLs, so that local builds don't depend on the network. In CI, always check them.
    if (!ci.isPresent) args("--offline")

    // Files that Markdown links to but that aren't Markdown aren't inputs. Creating or deleting one therefore doesn't
    // rerun the check locally; CI always runs it.
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
