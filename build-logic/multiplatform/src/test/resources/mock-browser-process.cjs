const assert = require("node:assert/strict")
const { createRequire } = require("node:module")
const path = require("node:path")
const childProcess = require("node:child_process")
const requireFromProject = createRequire(path.join(process.cwd(), "package.json"))

// Make Linux discovery independent of installed browsers and the host PATH. The real
// launcher still chooses its default command; Windows/macOS have built-in fallback paths.
const which = requireFromProject("which")
const originalWhich = which.sync
let launcher
try {
  which.sync = (command) => command
  launcher = requireFromProject("karma-chrome-launcher")["launcher:ChromeHeadless"][1].prototype
} finally {
  which.sync = originalWhich
}

childProcess.spawnSync = (executable, args, options) => {
  const explicit = process.env.TEST_TASK_CHROME_BIN
  const expected = explicit || launcher.DEFAULT_CMD[process.platform]
  assert.ok(expected, "The real launcher must select a default browser command")
  if (process.platform === "win32") {
    assert.equal(executable, "powershell.exe")
    assert.deepEqual(args.slice(0, 3), ["-NoProfile", "-NonInteractive", "-Command"])
    assert.equal(args.length, 4)
    assert.match(args[3], /Get-Command.*\$env:KARMA_CHROME_VERSION_EXECUTABLE/)
    assert.match(args[3], /Get-Item -LiteralPath.*VersionInfo\.ProductVersion/)
    assert.equal(options.env.KARMA_CHROME_VERSION_EXECUTABLE, expected)
  } else {
    assert.equal(executable, expected, "Probe must execute the browser selected by Karma and the task environment")
    assert.deepEqual(args, ["--version"])
  }
  assert.equal(options.encoding, "utf8")
  assert.equal(options.timeout, 30_000)
  assert.ok(!options.shell, "The executable must be started directly")
  return {
    status: 0,
    stdout: explicit ? process.env.TEST_EXPLICIT_CHROME_VERSION : process.env.TEST_DEFAULT_CHROME_VERSION,
    stderr: "",
  }
}
