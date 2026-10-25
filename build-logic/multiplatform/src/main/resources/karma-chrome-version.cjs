const { spawnSync } = require("node:child_process")

// Reuse the same launcher and selection rule as Karma instead of duplicating its platform-specific search paths.
const Launcher = require("karma-chrome-launcher")["launcher:ChromeHeadless"][1]
const { ENV_CMD, DEFAULT_CMD } = Launcher.prototype
const executable = process.env[ENV_CMD] || DEFAULT_CMD[process.platform]

if (executable) {
  const result = spawnSync(executable, ["--version"], { encoding: "utf8" })
  if (result.error) throw result.error
  if (result.status !== 0) {
    throw new Error(`Browser version probe failed: ${result.stderr}`)
  }
  process.stdout.write(result.stdout.trim())
}
