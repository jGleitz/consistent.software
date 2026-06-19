import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

abstract class SymlinkTask: DefaultTask() {
  abstract val target: FileSystemLocationProperty<*>
  abstract val link: FileSystemLocationProperty<*>

  @TaskAction
  fun createLink() {
    val linkPath = link.get().asFile.toPath()
    val targetPath = target.get().asFile.toPath()
    val targetRelativePath = linkPath.parent.relativize(targetPath)

    Files.deleteIfExists(linkPath)
    Files.createSymbolicLink(linkPath, targetRelativePath)
  }
}

abstract class SymlinkFileTask: SymlinkTask() {
  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract override val target: RegularFileProperty

  @get:OutputFile
  abstract override val link: RegularFileProperty
}

abstract class SymlinkDirTask: SymlinkTask() {
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  abstract override val target: DirectoryProperty

  @get:OutputDirectory
  abstract override val link: DirectoryProperty
}

