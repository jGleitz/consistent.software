import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

/** Shares completed browser revisions between Gradle processes, rebuilding interrupted installations. */
internal object ManagedChromeInstaller {
  // FileChannel.lock blocks other processes but throws for overlapping locks in this JVM.
  @Synchronized
  fun executable(
    cacheDirectory: File,
    platform: String,
    version: String,
    download: (File) -> File,
  ): File {
    require(Regex("[a-zA-Z0-9_-]+").matches(platform)) { "Invalid browser platform: $platform" }
    require(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+").matches(version)) { "An exact Chrome for Testing version is required" }
    cacheDirectory.mkdirs()
    return FileChannel.open(cacheDirectory.resolve(".install.lock").toPath(), CREATE, WRITE).use { channel ->
      channel.lock().use {
        val directory = cacheDirectory.resolve("$platform/$version").canonicalFile
        val marker = directory.resolve(".complete")
        if (marker.isFile) {
          val browser = directory.resolve(marker.readText()).canonicalFile
          if (browser.isFile && browser.toPath().startsWith(directory.toPath())) return browser
        }
        // An interrupted extraction can contain the executable while omitting DLLs or other runtime files.
        check(directory.deleteRecursively()) { "Could not remove incomplete browser installation $directory" }
        directory.mkdirs()
        val browser = download(directory).canonicalFile
        check(browser.isFile && browser.toPath().startsWith(directory.toPath())) { "Browser installer returned an invalid executable: $browser" }
        marker.writeText(browser.relativeTo(directory).path)
        browser
      }
    }
  }
}
