import java.io.File
import java.nio.file.Files

/** A separate JVM exercises the production OS lock, rather than only its in-process monitor. */
object ManagedChromeInstallerProbe {
  @JvmStatic
  fun main(args: Array<String>) {
    val cache = File(args[0])
    cache.resolve("started-${ProcessHandle.current().pid()}").writeText("")
    ManagedChromeInstaller.executable(cache, "test-platform", args[1]) { directory ->
      val deadline = System.nanoTime() + 5_000_000_000L
      while (cache.listFiles().orEmpty().count { it.name.startsWith("started-") } < 2) {
        check(System.nanoTime() < deadline) { "Other installer process did not start" }
        Thread.sleep(10)
      }
      val active = cache.resolve("active")
      Files.createFile(active.toPath())
      try {
        cache.resolve("downloads").appendText("${args[1]}\n")
        Thread.sleep(200)
        directory.resolve("browser").apply { writeText("browser") }
      } finally {
        active.delete()
      }
    }
  }
}
