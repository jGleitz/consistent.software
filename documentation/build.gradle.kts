import com.github.gradle.node.NodePlugin
import com.github.gradle.node.pnpm.task.PnpmInstallTask
import com.github.gradle.node.pnpm.task.PnpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  id("nodejs")
  id("format")
}

val pnpmInstall = nodeJs.registerPnpmInstall()

val buildVitepressSite = tasks.register<PnpmTask>("buildVitepressSite") {
  args = listOf("build")
  dependsOn(pnpmInstall)

  inputs.dir(project.layout.projectDirectory.dir(".vitepress")).withPathSensitivity(RELATIVE)
  inputs.dir(project.layout.projectDirectory.dir("src")).withPathSensitivity(RELATIVE)
  inputs.file(project.layout.projectDirectory.file("pnpm-lock.yaml")).withPathSensitivity(RELATIVE)
  inputs.file(project.layout.projectDirectory.file("package.json")).withPathSensitivity(RELATIVE)
  outputs.dir(project.layout.buildDirectory.dir("site"))
  outputs.cacheIf { true }
}

val assemble =
  tasks.named("assemble") {
    dependsOn(buildVitepressSite)
  }

tasks.named("build") {
  dependsOn(assemble)
}
