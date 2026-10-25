import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** Resolves the repository's ChromeHeadless launcher only when Gradle snapshots the test task's inputs. */
abstract class KarmaChromeVersion : ValueSource<String, KarmaChromeVersion.Parameters> {
  interface Parameters : ValueSourceParameters {
    val nodeExecutable: Property<String>
    val workingDirectory: DirectoryProperty
    val environment: MapProperty<String, String>
  }

  @get:Inject
  abstract val execOperations: ExecOperations

  override fun obtain(): String? {
    val script = checkNotNull(javaClass.getResource("/karma-chrome-version.cjs")).readText()
    val output = ByteArrayOutputStream()
    execOperations.exec {
      // Standard input avoids Windows command-line quoting changing the JavaScript source.
      commandLine(parameters.nodeExecutable.get(), "--input-type=commonjs")
      standardInput = script.byteInputStream()
      workingDir(parameters.workingDirectory.get())
      environment(parameters.environment.get())
      standardOutput = output
    }
    return output.toString(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
  }
}
