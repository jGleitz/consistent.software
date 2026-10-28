import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** The Node.js installation that mise selects for the whole build. */
abstract class NodeJsService
  @Inject
  constructor(
    private val execOperations: ExecOperations,
  ) : BuildService<NodeJsService.Parameters> {
    interface Parameters : BuildServiceParameters {
      val nodeExecutable: Property<String>
      val npmExecutable: Property<String>
    }

    // The probe runs in a Lazy, so that it blocks only callers for the same executable and not the map.
    private val runtimes = ConcurrentHashMap<String, Lazy<String>>()

    // Node reports what actually runs, including the architecture under emulation, which the JVM's system properties may not.
    fun runtimeOf(executable: String): String {
      // A task may resolve a relative executable with its own `PATH` or working directory, so only an absolute path identifies
      // the runtime that the task launches.
      require(File(executable).isAbsolute) { "Expected an absolute path to the Node.js executable, got '$executable'" }
      return runtimes.computeIfAbsent(executable) {
        lazy {
          val output = ByteArrayOutputStream()
          execOperations.exec {
            commandLine(executable, "-p", "JSON.stringify({version:process.version,platform:process.platform,arch:process.arch})")
            standardOutput = output
          }
          output.toString().trim()
        }
      }.value
    }
  }
