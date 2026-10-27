import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

/** Serialises our CLI invocations across tasks and Gradle processes sharing a Puppeteer cache. */
internal object BrowserInstallationLock {
  // FileChannel.lock blocks other processes but throws for overlapping locks in the same JVM.
  @Synchronized
  fun <T> withLock(cacheDirectory: File, action: () -> T): T {
    cacheDirectory.mkdirs()
    return FileChannel.open(cacheDirectory.resolve(".gradle-install.lock").toPath(), CREATE, WRITE).use { channel ->
      channel.lock().use { action() }
    }
  }
}
