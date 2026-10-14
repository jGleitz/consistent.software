import com.github.gradle.node.pnpm.task.PnpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  id("nodejs")
  id("format")
}

val pnpmInstall = nodeJs.registerPnpmInstall()

val buildVitepressSite =
  tasks.register<PnpmTask>("buildVitepressSite") {
    args = listOf("build")
    dependsOn(pnpmInstall)

    inputs.dir(project.layout.projectDirectory.dir(".vitepress")).withPathSensitivity(RELATIVE)
    inputs.dir(project.layout.projectDirectory.dir("src")).withPathSensitivity(RELATIVE)
    inputs.file(rootProject.layout.projectDirectory.file("pnpm-lock.yaml")).withPathSensitivity(RELATIVE)
    inputs.file(project.layout.projectDirectory.file("package.json")).withPathSensitivity(RELATIVE)
    outputs.dir(project.layout.buildDirectory.dir("site"))
    outputs.cacheIf { true }
  }

val checkSiteLinks =
  tasks.register<PnpmTask>("checkSiteLinks") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks links and fragments in every page of the built documentation site."
    dependsOn(buildVitepressSite)
    args = listOf("run", "check-site-links")
    // Locally, skip external URLs. In CI, always check them.
    if (!providers.environmentVariable("CI").isPresent) {
      args.addAll("--skip", "^https?://")
    }
  }

val assemble =
  tasks.named("assemble") {
    dependsOn(buildVitepressSite)
  }

tasks.named("build") {
  dependsOn(assemble)
}

tasks.named("check") {
  dependsOn(checkSiteLinks)
}
