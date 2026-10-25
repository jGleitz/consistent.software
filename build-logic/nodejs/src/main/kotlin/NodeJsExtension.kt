import org.gradle.api.Project
import org.gradle.api.provider.Provider

open class NodeJsExtension(project: Project) {
  val nodeExecutable: Provider<String> = project.miseWhich("node")
  val npmExecutable: Provider<String> = project.miseWhich("npm")
}

private fun Project.miseWhich(tool: String): Provider<String> =
  providers
    .exec {
      commandLine("mise", "which", tool)
      workingDir(rootDir)
    }.standardOutput.asText
    .map { it.trim() }
