import com.github.gradle.node.pnpm.task.PnpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  id("format")
  id("com.github.node-gradle.node") version "7.1.0"
}

tasks.pnpmSetup.configure {
  enabled = false
}

val buildVitepressSite by tasks.registering(PnpmTask::class) {
  args = listOf("build")
  dependsOn(tasks.pnpmInstall)

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
