import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
  id("nodejs")
  id("format")
  alias(buildLibs.plugins.kotlin.multiplatform) apply false
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL

  finalizedBy(tasks.spotlessPropertiesApply)
}

plugins.withType<NodeJsRootPlugin> {
  kotlinNodeJsEnvSpec.apply {
    download = false
    command = nodeJs.nodeExecutable
  }

  configure<NpmExtension> {
    command = nodeJs.npmExecutable
    lockFileDirectory = layout.projectDirectory.dir("gradle/kotlin-js-store")
  }
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

val pnpmInstall =
  tasks.register<Exec>("pnpmInstall") {
    commandLine("mise", "exec", "--", "pnpm", "install", "--frozen-lockfile", "--loglevel=warn")
    inputs.file("pnpm-workspace.yaml")
    inputs.file("pnpm-lock.yaml")
    inputs.files("mise.toml", "mise.lock").withPathSensitivity(PathSensitivity.RELATIVE)
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
    // Skip external URLs unless -PcheckExternalLinks=true, so that local builds don't depend on the network.
    val checkExternalLinks = providers.gradleProperty("checkExternalLinks").map(String::toBoolean).getOrElse(false)
    if (!checkExternalLinks) args("--offline")

    // Files that Markdown links to but that aren't Markdown aren't inputs. Creating or deleting one therefore doesn't
    // rerun an offline check; a check of external URLs always runs.
    inputs.files(linkCheckSources).withPropertyName("linkCheckSources").withPathSensitivity(PathSensitivity.RELATIVE)
    val marker = layout.buildDirectory.file("checkLinks/success")
    outputs.file(marker).withPropertyName("marker")
    // External URLs can break without any input changing, so a check of external URLs always runs.
    outputs.upToDateWhen { !checkExternalLinks }
    outputs.cacheIf { !checkExternalLinks }
    doLast { marker.get().asFile.writeText("") }
  }

tasks.check {
  dependsOn(checkLinks)
}
