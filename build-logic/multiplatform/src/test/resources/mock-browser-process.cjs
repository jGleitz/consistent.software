const assert = require("node:assert/strict")
const { existsSync } = require("node:fs")
const { createRequire } = require("node:module")
const path = require("node:path")
const childProcess = require("node:child_process")
const requireFromProject = createRequire(path.join(process.cwd(), "package.json"))

// Supply browser availability without depending on the host PATH. The real launcher
// still chooses its default command; Windows/macOS have built-in fallback paths.
const which = requireFromProject("which")
const originalWhich = which.sync
let launcher
try {
  which.sync = (command) => command
  launcher = requireFromProject("karma-chrome-launcher")["launcher:ChromeHeadless"][1].prototype
} finally {
  which.sync = originalWhich
}

const explicit = process.env.TEST_TASK_CHROME_BIN
const expected = explicit || launcher.DEFAULT_CMD[process.platform]
assert.ok(expected, "The real launcher must select a default browser command")

if (process.argv.includes("--print-executable")) {
  which.sync = (executable, options) => {
    assert.equal(executable, expected, "Resolve the browser selected by Karma and the task environment")
    assert.deepEqual(options, { nothrow: true })
    const filename = process.env.TEST_BROWSER_FILENAME || (explicit ? "explicit-browser.exe" : "default-browser.exe")
    const browser = path.join(__dirname, filename)
    return existsSync(browser) ? browser : null
  }
}

childProcess.spawnSync = (executable, args, options) => {
  assert.ok(!process.argv.includes("--print-executable"), "File fingerprinting must not launch a process")
  assert.equal(executable, expected, "Probe must execute the browser selected by Karma and the task environment")
  assert.deepEqual(args, ["--version"])
  assert.equal(options.encoding, "utf8")
  assert.equal(options.timeout, 30_000)
  assert.ok(!options.shell, "The executable must be started directly")
  return {
    status: 0,
    stdout: explicit ? process.env.TEST_EXPLICIT_CHROME_VERSION : process.env.TEST_DEFAULT_CHROME_VERSION,
    stderr: "",
  }
}
