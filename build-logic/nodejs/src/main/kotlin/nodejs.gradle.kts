import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmSetupTask
import com.github.gradle.node.pnpm.task.PnpmSetupTask
import com.github.gradle.node.task.NodeSetupTask
import com.github.gradle.node.util.Platform
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.variant.computeNodeExec

val nodeJsExtension = project.extensions.create<NodeJsExtension>("nodeJs", project)

if (project.path == rootProject.path) {
  project.plugins.apply(NodePlugin::class.java)
} else {
  project.extensions.create<NodeExtension>("__nodeplugindonotuse", project)
}

NodeExtension[project].apply {
  version = nodeJsExtension.nodeJsVersion
  // TODO define the version of pnpm and have it updated by renovate

  // we don’t actually download anything in child projects, but the node plugin uses this property to
  // decide which file layout it expects
  download = true
  enableTaskRules = false

  pnpmWorkDir = rootProject.projectDir.resolve(".gradle").resolve("pnpm")
}

if (project.path == rootProject.path) {
  project.plugins.apply(NodePlugin::class.java)
  val nodeExtension = NodeExtension[project]
  val nodePath = VariantComputer()

  val nodeDir = nodeExtension.resolvedNodeDir
  val nodeBinDir = nodePath.computeNodeBinDir(nodeExtension.resolvedNodeDir, nodeExtension.resolvedPlatform)
  val nodeExecutable = computeNodeExec(nodeExtension, nodeBinDir).map(::File)

  val linkNodeInstallation =
    project.tasks.register<SymlinkDirTask>("linkNodeInstallation") {
      group = NodePlugin.NODE_GROUP
      dependsOn(NodeSetupTask.NAME)
      mustRunAfter(NpmSetupTask.NAME, PnpmSetupTask.NAME)
      target = nodeDir
      link = nodeJsExtension.nodeDir
    }

  val linkNodeBinary =
    project.tasks.register<SymlinkFileTask>("linkNodeBinary") {
      group = NodePlugin.NODE_GROUP
      dependsOn(NodeSetupTask.NAME)
      mustRunAfter(NpmSetupTask.NAME, PnpmSetupTask.NAME)
      target = nodeExecutable
      link = nodeJsExtension.nodeExecutable
    }

  val linkNode =
    project.tasks.register(NodeJsExtension.SETUP_TASK_NAME) {
      group = NodePlugin.NODE_GROUP
      dependsOn(linkNodeInstallation, linkNodeBinary)
    }

  val npmDir = nodePath.computeNpmDir(nodeExtension, nodeDir)
  val npmBinDir = nodePath.computeNpmBinDir(npmDir, nodeExtension.resolvedPlatform)
  val npmExecutable = nodePath.computeNpmExec(nodeExtension, npmBinDir).map(::File)
  project.tasks.register<SymlinkFileTask>(NodeJsExtension.NPM_SETUP_TASK_NAME) {
    group = NodePlugin.NPM_GROUP
    dependsOn(linkNode, NpmSetupTask.NAME)
    mustRunAfter(PnpmSetupTask.NAME)
    target = npmExecutable
    link = nodeJsExtension.npmExecutable
  }

  val pnpmDir = nodePath.computePnpmDir(nodeExtension)
  val pnpmBinDir = nodePath.computePnpmBinDir(pnpmDir, nodeExtension.resolvedPlatform)
  val pnpmExecutable = nodePath.computePnpmExec(nodeExtension, pnpmBinDir).map(::File)
  project.tasks.register<SymlinkFileTask>(NodeJsExtension.PNPM_SETUP_TASK_NAME) {
    group = NodePlugin.PNPM_GROUP
    dependsOn(linkNode, PnpmSetupTask.NAME)
    mustRunAfter(NpmSetupTask.NAME)
    target = pnpmExecutable
    link = nodeJsExtension.pnpmExecutable
  }
} else {
  NodeExtension[project].apply {
    resolvedNodeDir = nodeJsExtension.nodeDir
    // We hard-code the platform because the node plugin accesses it. It does not influence the plugin’s behaviour
    // because we set absolute paths to the executables below
    resolvedPlatform = Platform("windows", "x64")
    // pnpmCommand = nodeJsExtension.pnpmExecutable.map { it.asFile.absolutePath }
  }
}
