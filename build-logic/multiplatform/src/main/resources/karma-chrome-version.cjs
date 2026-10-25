const { spawnSync } = require("node:child_process")

// Reuse the same launcher and selection rule as Karma instead of duplicating its platform-specific search paths.
const Launcher = require("karma-chrome-launcher")["launcher:ChromeHeadless"][1]
const { ENV_CMD, DEFAULT_CMD } = Launcher.prototype
const executable = process.env[ENV_CMD] || DEFAULT_CMD[process.platform]

if (executable) {
  // Windows Chrome is a GUI executable: --version does not reliably write to stdout or exit.
  // Read the selected executable's version resource instead, resolving command names through PATH.
  const result = process.platform === "win32"
    ? spawnSync("powershell.exe", [
        "-NoProfile", "-NonInteractive", "-Command",
        "$browser = Get-Command -Name $env:KARMA_CHROME_VERSION_EXECUTABLE -CommandType Application -ErrorAction Stop; " +
          "(Get-Item -LiteralPath $browser.Source -ErrorAction Stop).VersionInfo.ProductVersion",
      ], {
        encoding: "utf8",
        timeout: 30_000,
        env: { ...process.env, KARMA_CHROME_VERSION_EXECUTABLE: executable },
      })
    : spawnSync(executable, ["--version"], { encoding: "utf8", timeout: 30_000 })
  if (result.error) throw result.error
  if (result.status !== 0) {
    throw new Error(`Browser version probe failed: ${result.stderr}`)
  }
  process.stdout.write(result.stdout.trim())
}
