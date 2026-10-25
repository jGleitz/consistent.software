import org.gradle.api.Project
import org.gradle.api.provider.Provider

open class NodeJsExtension(project: Project) {
  val nodeExecutable: Provider<String> = project.miseWhich("node")

  // On Windows, `mise which npm` names the shell script for POSIX shells, which Java cannot start. Node.js ships npm.cmd next to it.
  val npmExecutable: Provider<String> =
    project.miseWhich("npm").zip(project.providers.systemProperty("os.name")) { npm, os ->
      if (os.startsWith("Windows")) "$npm.cmd" else npm
    }
}

private fun Project.miseWhich(tool: String): Provider<String> =
  providers
    .exec {
      commandLine("mise", "which", tool)
      workingDir(rootDir)
    }.standardOutput.asText
    .map { it.trim() }
