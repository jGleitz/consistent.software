import assert from "node:assert/strict"
import { spawn } from "node:child_process"
import { existsSync } from "node:fs"
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises"
import { createRequire } from "node:module"
import { tmpdir } from "node:os"
import { dirname, join } from "node:path"
import test from "node:test"

const require = createRequire(join(process.cwd(), "package.json"))
const browsers = require("@puppeteer/browsers")
const bootstrap = `data:text/javascript;base64,${Buffer.from(await readFile(process.env.TEST_BOOTSTRAP)).toString("base64")}`
const dataUrl = (source) => `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`
// Keep the real path calculation, uninstaller and proper-lockfile. Only replace the network download/extraction.
const mock = dataUrl(`
  import { createRequire } from "node:module";
  import { mkdir, open, rm, writeFile, appendFile } from "node:fs/promises";
  import { dirname, join } from "node:path";
  const require = createRequire(process.env.TEST_NPM_ROOT + "/package.json");
  const Module = require("node:module");
  const original = Module._load;
  const real = require("@puppeteer/browsers");
  const locking = require("proper-lockfile");
  Module._load = function(name, ...args) {
    if (name === "proper-lockfile") return locking;
    if (name !== "@puppeteer/browsers") return original.call(this, name, ...args);
    return { ...real, install: async (options) => {
      const active = join(options.cacheDir, "active-installation");
      const handle = await open(active, "wx"); // Concurrent installation is a test failure.
      try {
        await appendFile(join(options.cacheDir, "downloads"), options.buildId + "\\n");
        const executablePath = real.computeExecutablePath(options);
        await mkdir(dirname(executablePath), { recursive: true });
        await writeFile(executablePath, "browser");
        if (process.env.TEST_FAIL_INSTALL) throw new Error("interrupted extraction");
        await new Promise(resolve => setTimeout(resolve, 250));
        await writeFile(join(dirname(executablePath), "runtime-file"), "complete");
        return { executablePath };
      } finally {
        await handle.close();
        await rm(active);
      }
    }};
  };
`)

test("parallel checkouts share one completed revision and recover interrupted extraction", async () => {
  const root = await mkdtemp(join(tmpdir(), "managed chrome "))
  const cacheDir = join(root, "shared cache")
  const checkouts = [join(root, "first checkout"), join(root, "second checkout")]
  await Promise.all(checkouts.map((path) => mkdir(path)))
  const platform = browsers.detectBrowserPlatform()
  const first = "156.0.8078.47"
  const second = "156.0.8078.48"
  const executable = (buildId) => browsers.computeExecutablePath({ browser: browsers.Browser.CHROME, buildId, platform, cacheDir })
  const run = (buildId, cwd, fail = false) => new Promise((resolve) => {
    const process = spawn(globalThis.process.execPath, ["--import", mock, "--import", bootstrap, "-e", "console.log(process.env.CHROME_BIN)"], {
      cwd,
      env: {
        ...globalThis.process.env,
        TEST_NPM_ROOT: globalThis.process.cwd(),
        TEST_FAIL_INSTALL: fail ? "true" : "",
        CONSISTENT_BROWSER_CACHE: cacheDir,
        CONSISTENT_CHROME_VERSION: buildId,
        CHROME_BIN: "unused system browser",
      },
    })
    let output = ""
    process.stdout.on("data", (data) => { output += data })
    process.stderr.on("data", (data) => { output += data })
    process.on("close", (code) => resolve({ code, output: output.trim() }))
    process.on("error", (error) => resolve({ code: -1, output: String(error) }))
  })
  try {
    const initial = await Promise.all(checkouts.map((cwd) => run(first, cwd)))
    for (const result of initial) {
      assert.equal(result.code, 0, result.output)
      assert.equal(result.output, executable(first))
    }
    assert.equal(await readFile(join(cacheDir, "downloads"), "utf8"), first + "\n")
    assert.equal(existsSync(join(cacheDir, ".chrome.lock")), false)

    const failed = await run(second, checkouts[0], true)
    assert.notEqual(failed.code, 0)
    assert.equal(existsSync(executable(second)), true)
    assert.equal(existsSync(join(dirname(executable(second)), "runtime-file")), false)
    const repaired = await run(second, checkouts[1])
    assert.equal(repaired.code, 0, repaired.output)
    assert.equal(await readFile(join(dirname(executable(second)), "runtime-file"), "utf8"), "complete")

    const reused = await run(first, checkouts[1])
    assert.equal(reused.code, 0, reused.output)
    assert.equal(await readFile(join(cacheDir, "downloads"), "utf8"), [first, second, second, ""].join("\n"))
    const invalid = await run("stable", checkouts[0])
    assert.notEqual(invalid.code, 0)
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})
