import org.gradle.api.Task
import org.gradle.api.provider.Provider

open class NodeJsExtension(
  private val service: Provider<NodeJsService>,
) {
  val nodeExecutable: Provider<String> = service.flatMap { it.parameters.nodeExecutable }

  val npmExecutable: Provider<String> = service.flatMap { it.parameters.npmExecutable }

  /** Makes the runtime of the Node [executable] that [task] launches an input of [task]. */
  fun runtimeInput(
    task: Task,
    executable: Provider<String>,
  ) {
    task.usesService(service)
    task.inputs.property("nodeJsRuntime", service.zip(executable) { nodeJs, node -> nodeJs.runtimeOf(node) })
  }
}
