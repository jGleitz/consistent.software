val service =
  gradle.sharedServices.registerIfAbsent("nodeJs", NodeJsService::class) {
    parameters.nodeExecutable = mise("which", "node")
    // On Windows, `mise which npm` names the shell script for POSIX shells, which Java cannot start. Node.js ships npm.cmd next to it.
    parameters.npmExecutable =
      mise("which", "npm").zip(providers.systemProperty("os.name")) { npm, os ->
        if (os.startsWith("Windows")) "$npm.cmd" else npm
      }
  }

project.extensions.create<NodeJsExtension>("nodeJs", service)

fun mise(vararg arguments: String): Provider<String> =
  providers
    .exec {
      commandLine("mise", *arguments)
      workingDir(rootDir)
    }.standardOutput.asText
    .map { it.trim() }
