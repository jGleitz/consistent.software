import { existsSync } from "node:fs"
import { mkdir, rm, writeFile } from "node:fs/promises"
import { createRequire } from "node:module"
import { homedir } from "node:os"
import { join } from "node:path"

// KMP installs these devDependencies in the test package's npm resolution.
const require = createRequire(join(process.cwd(), "package.json"))
const { Browser, computeExecutablePath, detectBrowserPlatform, install, uninstall } = require("@puppeteer/browsers")
const { lock } = require("proper-lockfile")
const buildId = process.env.CONSISTENT_CHROME_VERSION
if (!/^\d+\.\d+\.\d+\.\d+$/.test(buildId ?? "")) throw new Error("An exact Chrome for Testing version is required")
const platform = detectBrowserPlatform()
if (!platform) throw new Error("Chrome for Testing does not support this platform")

const cacheRoot = process.env.XDG_CACHE_HOME || join(homedir(), ".cache")
const cacheDir = process.env.CONSISTENT_BROWSER_CACHE || join(cacheRoot, "consistent.software", "browsers")
await mkdir(cacheDir, { recursive: true })
// Different modules, Gradle processes and checkouts share this installation. The installer itself has no process lock.
const release = await lock(cacheDir, {
  realpath: false,
  lockfilePath: join(cacheDir, ".chrome.lock"),
  retries: { retries: 300, minTimeout: 1000, maxTimeout: 1000 },
})
try {
  const options = { browser: Browser.CHROME, buildId, platform, cacheDir }
  const complete = join(cacheDir, `.${platform}-${buildId}.complete`)
  let executablePath = computeExecutablePath(options)
  if (!existsSync(complete) || !existsSync(executablePath)) {
    // An interrupted extraction can leave an executable but omit other runtime files. Rebuild unless installation completed.
    await rm(complete, { force: true })
    await uninstall(options)
    executablePath = (await install(options)).executablePath
    await writeFile(complete, "")
  }
  process.env.CHROME_BIN = executablePath
} finally {
  // Tests can run concurrently after installation; keep only the download and extraction serialised.
  await release()
}
