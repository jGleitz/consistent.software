const { spawnSync } = require("node:child_process")

function resolveExecutable(executable) {
  // Resolve command-name CHROME_BIN values just as the launcher's process invocation would.
  return require("which").sync(executable, { nothrow: true })
}

function readVersion(executable) {
  const result = spawnSync(executable, ["--version"], { encoding: "utf8", timeout: 30_000 })
  if (result.error) throw result.error
  if (result.status !== 0) throw new Error(`Browser version probe failed: ${result.stderr}`)
  return result.stdout.trim()
}

// Reuse the same launcher and selection rule as Karma instead of duplicating its platform-specific search paths.
const Launcher = require("karma-chrome-launcher")["launcher:ChromeHeadless"][1]
const { ENV_CMD, DEFAULT_CMD } = Launcher.prototype
const executable = process.env[ENV_CMD] || DEFAULT_CMD[process.platform]

if (executable) {
  const identity = process.argv.includes("--print-executable") ? resolveExecutable(executable) : readVersion(executable)
  if (identity) process.stdout.write(identity)
}
