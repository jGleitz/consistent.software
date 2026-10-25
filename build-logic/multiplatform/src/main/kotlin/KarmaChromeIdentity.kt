import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

/** Probes ChromeHeadless during input snapshotting: an executable path on Windows, a browser version elsewhere. */
abstract class KarmaChromeIdentity : ValueSource<String, KarmaChromeIdentity.Parameters> {
  interface Parameters : ValueSourceParameters {
    val nodeExecutable: Property<String>
    val workingDirectory: DirectoryProperty
    val environment: MapProperty<String, String>
    val resolveExecutable: Property<Boolean>
  }

  @get:Inject
  abstract val execOperations: ExecOperations

  override fun obtain(): String? {
    val script = checkNotNull(javaClass.getResource("/karma-chrome-identity.cjs")).readText()
    val output = ByteArrayOutputStream()
    execOperations.exec {
      // Standard input avoids Windows command-line quoting changing the JavaScript source.
      commandLine(
        parameters.nodeExecutable.get(),
        "--input-type=commonjs",
        "-",
        if (parameters.resolveExecutable.get()) "--print-executable" else "--print-version",
      )
      standardInput = script.byteInputStream()
      workingDir(parameters.workingDirectory.get())
      environment(parameters.environment.get())
      standardOutput = output
    }
    return output.toString(UTF_8).trim().takeIf { it.isNotEmpty() }
  }
}
