// Runs linkinator like its CLI would, but quietly. The CLI logs every working link at
// its warning level, so its verbosity flag cannot hide them without hiding real
// warnings, too. This script prints warnings as they occur and the full report only if
// the check fails.
import { parseArgs } from "node:util"
import { LinkChecker, LinkState, resetSharedAgents } from "linkinator"

const { values } = parseArgs({
  options: { skip: { type: "string", multiple: true, default: [] } },
})

const checker = new LinkChecker()
checker.on("retry", ({ url, secondsUntilRetry }) =>
  console.warn(`Retrying: ${url} in ${secondsUntilRetry} seconds.`),
)
checker.on("redirect", ({ url, targetUrl, status }) =>
  console.warn(`[REDIRECT] ${url} → ${targetUrl} (${status})`),
)
checker.on("statusCodeWarning", ({ url, status }) => console.warn(`[WARN] ${url} (${status})`))

try {
  const result = await checker.check({
    path: "**/*.html",
    serverRoot: "build/site",
    recurse: true,
    cleanUrls: true,
    checkFragments: true,
    timeout: 30000,
    linksToSkip: values.skip,
  })
  if (!result.passed) {
    for (const link of result.links) {
      console.error(`[${link.state} ${link.status ?? ""}] ${link.url} (on ${link.parent ?? "-"})`)
      if (link.state === LinkState.BROKEN) console.error(link.failureDetails)
    }
    const broken = result.links.filter((link) => link.state === LinkState.BROKEN)
    console.error(`Detected ${broken.length} broken links.`)
    process.exitCode = 1
  }
} finally {
  resetSharedAgents()
}
