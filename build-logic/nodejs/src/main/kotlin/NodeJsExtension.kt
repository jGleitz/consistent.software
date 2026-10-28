import org.gradle.api.Project
import org.gradle.api.provider.Provider

open class NodeJsExtension(project: Project) {
  private val providers = project.providers

  val nodeExecutable: Provider<String> = project.mise("which", "node")

  // Node reports what actually runs, including the architecture under emulation, which the JVM's system properties may not.
  fun runtimeOf(executable: Provider<String>): Provider<String> =
    executable.flatMap { node ->
      providers
        .exec {
          commandLine(node, "-p", "JSON.stringify({version:process.version,platform:process.platform,arch:process.arch})")
        }.standardOutput.asText
        .map { it.trim() }
    }

  // On Windows, `mise which npm` names the shell script for POSIX shells, which Java cannot start. Node.js ships npm.cmd next to it.
  val npmExecutable: Provider<String> =
    project.mise("which", "npm").zip(project.providers.systemProperty("os.name")) { npm, os ->
      if (os.startsWith("Windows")) "$npm.cmd" else npm
    }
}

private fun Project.mise(vararg arguments: String): Provider<String> =
  providers
    .exec {
      commandLine("mise", *arguments)
      workingDir(rootDir)
    }.standardOutput.asText
    .map { it.trim() }
