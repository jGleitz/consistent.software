import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File

open class NodeJsExtension(project: Project) {
  val nodeExecutable: Provider<String> = project.miseWhich("node")
  val npmExecutable: Provider<String> = project.miseWhich("npm").map { executable ->
    // mise can select Node's extensionless POSIX npm script even on Windows.
    if (System.getProperty("os.name").startsWith("Windows") && File(executable).extension.isEmpty()) "$executable.cmd" else executable
  }
}

private fun Project.miseWhich(tool: String): Provider<String> =
  providers
    .exec {
      commandLine("mise", "which", tool)
      workingDir(rootDir)
    }.standardOutput.asText
    .map { it.trim() }
