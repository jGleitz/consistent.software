import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  id("nodejs")
  id("format")
}

val buildVitepressSite =
  tasks.register<Exec>("buildVitepressSite") {
    commandLine("mise", "exec", "--", "pnpm", "build")
    dependsOn(":pnpmInstall")

    inputs.dir(project.layout.projectDirectory.dir(".vitepress")).withPathSensitivity(RELATIVE)
    inputs.dir(project.layout.projectDirectory.dir("src")).withPathSensitivity(RELATIVE)
    inputs.file(rootProject.layout.projectDirectory.file("pnpm-lock.yaml")).withPathSensitivity(RELATIVE)
    inputs.files(rootProject.files("mise.toml", "mise.lock")).withPathSensitivity(RELATIVE)
    inputs.file(project.layout.projectDirectory.file("package.json")).withPathSensitivity(RELATIVE)
    outputs.dir(project.layout.buildDirectory.dir("site"))
    outputs.cacheIf { true }
  }

val checkSiteLinks =
  tasks.register<Exec>("checkSiteLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks links and fragments in every page of the built documentation site."
    dependsOn(buildVitepressSite)
    val site =
      layout.buildDirectory
        .dir("site")
        .get()
        .asFile
    commandLine(
      "mise",
      "exec",
      "--",
      "lychee",
      "--config",
      rootProject.file("lychee.toml").path,
      // resolve links like VitePress's clean URLs do
      "--root-dir",
      site.absolutePath,
      "--fallback-extensions",
      "html",
      "--index-files",
      "index.html",
      "--include-fragments",
      site.path,
    )
    // Skip external URLs unless -PcheckExternalLinks=true, so that local builds don't depend on the network.
    val checkExternalLinks = providers.gradleProperty("checkExternalLinks").map(String::toBoolean).getOrElse(false)
    if (!checkExternalLinks) args("--offline")
  }

val assemble =
  tasks.named("assemble") {
    dependsOn(buildVitepressSite)
  }

// The scanner only finds the sources of JVM and Kotlin projects by itself. SonarQube's TypeScript analysis skips hidden
// directories, so it doesn't analyse the files in .vitepress.
sonar {
  properties {
    property("sonar.sources", listOf(file(".vitepress"), file("src")))
  }
}

tasks.named("build") {
  dependsOn(assemble)
}

tasks.named("check") {
  dependsOn(checkSiteLinks)
}
