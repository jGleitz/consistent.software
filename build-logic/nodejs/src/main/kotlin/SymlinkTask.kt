import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.nio.file.Files

private const val SYMLINK_TASK_REASON =
  "Only the symlink is managed; its target's contents belong to the installation tasks."

@UntrackedTask(because = SYMLINK_TASK_REASON)
abstract class SymlinkTask: DefaultTask() {
  @get:Internal
  abstract val target: FileSystemLocationProperty<*>

  @get:Internal
  abstract val link: FileSystemLocationProperty<*>

  @TaskAction
  fun createLink() {
    val linkPath = link.get().asFile.toPath()
    val targetPath = target.get().asFile.toPath()
    val targetRelativePath = linkPath.parent.relativize(targetPath)

    if (Files.isSymbolicLink(linkPath) && Files.readSymbolicLink(linkPath) == targetRelativePath) {
      return
    }

    Files.createDirectories(linkPath.parent)
    Files.deleteIfExists(linkPath)
    Files.createSymbolicLink(linkPath, targetRelativePath)
  }
}

@UntrackedTask(because = SYMLINK_TASK_REASON)
abstract class SymlinkFileTask: SymlinkTask() {
  abstract override val target: RegularFileProperty
  abstract override val link: RegularFileProperty
}

@UntrackedTask(because = SYMLINK_TASK_REASON)
abstract class SymlinkDirTask: SymlinkTask() {
  abstract override val target: DirectoryProperty
  abstract override val link: DirectoryProperty
}
