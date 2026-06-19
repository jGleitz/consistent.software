import com.github.gradle.node.NodePlugin
import com.github.gradle.node.pnpm.task.PnpmInstallTask
import com.github.gradle.node.pnpm.task.PnpmSetupTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register

open class NodeJsExtension(private val project: Project) {
  val nodeJsVersion = project.objects.property<String>().convention(project.provider {
    project.rootProject.projectDir.resolve(".nvmrc").readText().trim()
  })

  /** a symlink to the current nodejs installation */
  val nodeDir = project.objects.directoryProperty().convention(
    project.rootProject.layout.projectDirectory.dir(".gradle").dir("nodejs").dir("current")
  )

  /** a directory containing symlinks to the nodejs-related executables */
  val nodeBinDir = project.objects.directoryProperty().convention(
    project.rootProject.layout.projectDirectory.dir(".gradle").dir("nodejs").dir("bin")
  )

  val nodeExecutable = nodeBinDir.map { it.file("node") }
  val npmExecutable = nodeBinDir.map { it.file("npm") }
  val pnpmExecutable = nodeBinDir.map { it.file("pnpm") }

  val setupTask = ":${SETUP_TASK_NAME}"
  val npmSetupTask = ":${NPM_SETUP_TASK_NAME}"
  val pnpmSetupTask = ":${PNPM_SETUP_TASK_NAME}"

  fun registerPnpmInstall() = with(project) {
    tasks.register(PnpmSetupTask.NAME) {
      group = NodePlugin.PNPM_GROUP
      description = "Sets up pnpm (by delegating to the root project)"
      dependsOn(pnpmSetupTask)
    }
    tasks.register("pnpmInstall", PnpmInstallTask::class)
  }

  internal companion object {
    const val SETUP_TASK_NAME = "linkNode"
    const val NPM_SETUP_TASK_NAME = "linkNpm"
    const val PNPM_SETUP_TASK_NAME = "linkPnpm"
  }
}
