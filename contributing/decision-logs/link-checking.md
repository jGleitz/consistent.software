# Link checking

Status: decided on 24 October 2026.
Audience: maintainers changing how the build checks links.

This log records which tools check our links, and why we discarded the alternatives we tried.

## Decision

- [lychee](https://lychee.cli.rs/) checks all external URLs: those in the Markdown files outside the documentation site (`checkExternalLinks`) and those in the built site (`checkSiteLinks`). It also checks the links and fragments within the built site. [mise](https://mise.jdx.dev/) installs it.
- [remark-validate-links](https://github.com/remarkjs/remark-validate-links) checks links to files and headings in the Markdown files outside the documentation site (`checkLinks`), with GitHub’s semantics.
- VitePress checks links between pages when it builds the site.

External URLs are only checked in CI, so that local builds don’t depend on the network.

## Why lychee

Wikipedia regularly answered our CI with HTTP 429 (Too Many Requests).
Wikimedia’s [User-Agent policy](https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_User-Agent_Policy) and [robot policy](https://wikitech.wikimedia.org/wiki/Robot_policy) ask bots to identify themselves with a descriptive User-Agent, to respect `Retry-After`, and to limit their concurrency.
Of the tools we tried, only lychee lets us do all of that:

- It sends the User-Agent we configure.
- It limits concurrency and request rate per host, so we can be gentle with Wikipedia without slowing down all other hosts.
- It adapts its backoff to rate-limit responses, and sends one GET per link.
- It checks Markdown files and the built site, so one tool covers both.

We don’t use lychee’s cache: it expires entries after a fixed age and ignores HTTP cache headers.
Wikipedia’s pages don’t allow caching anyway (`max-age=0`).

## Discarded Alternatives

- **lychee, in our first setup.** Before we used mise, installing lychee needed custom build code. We replaced it with remark plugins that the existing pnpm setup could install. With mise, that cost is gone.
- **[remark-lint-no-dead-urls](https://github.com/remarkjs/remark-lint-no-dead-urls)** (with [dead-or-alive](https://github.com/wooorm/dead-or-alive)) checked external URLs in Markdown. It timed out, crashed through a dependency, and was rate-limited. Its User-Agent imitates a browser, which Wikimedia treats as malicious, and is sent under a wrong header name. It doesn’t retry on HTTP 429 or respect `Retry-After`.
- **[linkinator](https://github.com/JustinBeckwith/linkinator)** crawled the built site. All its limits apply to every host at once. By default, it sends the generic User-Agent `node`, runs 100 requests in parallel, and sends a GET after every HEAD that fails.

## Accepted Gap

Content inside VitePress’s `<ClientOnly>` is not part of the built HTML, so no tool checks external URLs in it.
No page uses `<ClientOnly>` today.
