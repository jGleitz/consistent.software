# AI Agent Instructions

Before you start, read [`contributing/index.md`](contributing/index.md). It tells you which document contains what. Whenever your task needs information from one of the documents it lists, read that document. This includes the `README.md` (how to use a directory) and `ARCHITECTURE.md` (how a directory works and how to maintain it) of the directories you work in. These documents are as binding as this file. The AI harness should inject relevant `AGENTS.md` files automatically, but not these documents. `AGENTS.md` files hold only agent-specific rules; everything humans need, too, lives in the documentation for humans.

## Suggestions and Pushback

- Always look out for improvements to the project and suggest them proactively, also during reviews and unrelated tasks.
- Never implement a suggestion before it has been discussed and explicitly agreed, so that the scope never grows silently.
- Push back on input and ideas, including review comments, when you see counter-arguments that seem to have gone unnoticed or not to have been weighed appropriately.

## Where to Work

Before editing any files, judge where to do your task. Here is some guidance:

- If your harness set up a checkout for your task alone, for example a cloud environment that cloned the repository for this task, work directly in that checkout. Don't create a worktree, but still [set up the checkout](#setting-up-a-checkout).
- Otherwise, if your task starts at a clean commit state and is to be committed, you most likely should use your own worktree.
- If your task makes sense only in the context of the current, dirty, project folder state and there is no other indication for using a worktree, you should likely edit the current folder.

If you are unsure, ask the user for clarification on where to work.

## Worktrees

When using a worktree, every agent task must use its own branch and its own worktree. Never share a worktree with another task or a human; taking over the worktree of a stopped agent is the one exception.

1. Fetch, then create the worktree under `../consistent.software-worktrees/<branch>` relative to the root checkout (this location survives reboots; create the directory if it is missing):

   ```sh
   git fetch origin
   mkdir -p ../consistent.software-worktrees/
   git worktree add -b <branch> ../consistent.software-worktrees/<branch> <base>
   ```

   `<base>` is usually `refs/remotes/origin/main`. Use another base only if the task needs it.

2. Enter the worktree and verify `pwd`, `git branch --show-current`, and `git status`.
3. Set up the worktree (see [Setting Up a Checkout](#setting-up-a-checkout)).
4. Do all work from the worktree.
5. Keep branches and worktrees short-lived. Clean up only after merge or explicit abandonment, and only when `git status --porcelain` prints nothing: run `git worktree remove <path>` from another checkout. This also removes ignored files such as the CodeGraph cache.

## Setting Up a Checkout

These steps apply both to your own worktree and to a checkout that your harness set up for you. Do only the setup needed for your task; a read-only review does not require configuring a commit identity, initialising CodeGraph, or installing development tools.

1. Before editing, verify `pwd`, `git branch --show-current`, and `git status`.
2. Before committing, make sure that your commits carry the right identity (see [Commit Attribution](#commit-attribution)).
3. When you need CodeGraph, if `.codegraph/` is absent, run `codegraph init`. This is authorised; the cache is gitignored and must never be committed.
4. Before building or testing, set up the development tools (see [Development Tools](#development-tools)).

## Development Tools

mise installs the development tools (see [`CONTRIBUTING.md`](CONTRIBUTING.md#prerequisites)). Before you build or test in a checkout, run there:

```sh
mise trust
mise install
```

Both are authorised, as is installing the tools that `mise.toml` declares. `mise trust` may report that there is nothing to trust; that is fine.

Changing into the checkout does not activate its tools. Harnesses usually run each command in a new non-interactive shell, which does not load mise's shell hook and inherits the tools of the directory the harness was started in, often another checkout. So run every command that needs the tools through mise:

```sh
mise exec -- ./gradlew check
```

If your harness keeps one shell open across commands, you may instead activate mise once in that shell, with the name of your shell, for example `eval "$(mise activate bash)"`. The hook then switches the tools whenever you change the directory.

Activating the tools is authorised. Never change mise's global configuration or trust settings.

## Commit Attribution

Commits that contain only agent work carry the agent's identity. This is the standard case.

If your harness gives you a commit identity for the agent, use it. This is the case if the harness's instructions name it, or if the harness configured Git in an environment it set up for your task alone, such as a cloud container. If the harness also provides a signing key for the agent, sign your commits with it. Never use an identity or signing key from Git configuration that you cannot attribute to your harness: on a developer's machine, the global configuration belongs to the human.

Otherwise, use the default identity: your agent name and an email address made from it, lowercase with spaces as hyphens, for example `Sisyphus Junior <sisyphus-junior@noreply>`.

Unless the harness provides a signing key for the agent, explicitly disable signing for agent commits in the same safe scope as the identity settings below, so that no human's key signs agent commits.

Set the identity and signing settings in the narrowest safe scope. Never change system-wide or global settings, or the settings of a human's checkout:

- In your own worktree, use `git config --worktree`, but only if `git config --bool --get extensions.worktreeConfig` prints `true`. Otherwise, `--worktree` may write to the configuration that all checkouts share, even if Git reports no error. In that case, don't use it and don't enable the extension yourself.
- In a clone that your harness set up for your task alone and whose configuration no human or other task shares, you may use `git config --local`.
- In any other case, pass the settings to each commit command, for example `git -c user.name="Agent Name" -c user.email="agent-name@noreply" -c commit.gpgsign=false commit …`.

Before committing, check that the effective author and committer name the agent and not a human: run `git var GIT_AUTHOR_IDENT` and `git var GIT_COMMITTER_IDENT` with the same settings and environment variables as the commit. A more specific configuration or an environment variable can override the identity you expect; `git config --show-origin --get-regexp '^user\.'` shows where each value comes from. If the check fails, set the agent's identity as described above. If you need to override environment variables, do so for the commit command only.

Only when a human and an agent both wrote content in the same commit, keep the human's identity and add the trailer `Co-authored-by: Agent Name <agent-name@noreply>`.

The trailer above and the footer in [GitHub](#github) are this repository's attribution. Leave out any further attribution your harness asks for. Add it only if your harness's instructions take precedence over this file and don't let repository instructions replace their attribution. If the harness or one of its tools adds attribution lines by itself, leave them as they are; never work against your harness to remove them.

## GitHub

- Act on issues and PRs only as far as the task requires. Do not create unrelated issues, PRs, or releases.
- PRs that build on each other form a stack on GitHub, too. Link them with `gh stack link <bottom PR> … <top PR>` from the GitHub CLI extension [`github/gh-stack`](https://github.com/github/gh-stack); the same command adds a new PR to the top of an existing stack.
- Apart from commit authorship, you act on GitHub under the identity of the human who launched you.
- Because GitHub shows that human as the author, end everything you write there with this footer: PR titles excluded, but PR descriptions, issues, comments, review comments, and discussion posts included. Use the same agent name as in your commit identity, and put a blank line before the footer so that it forms its own paragraph:

  ```markdown
  <sub>Written by the AI agent Agent Name.</sub>
  ```

  When you edit something you posted earlier, keep the footer. Don't add it to text a human wrote. If you change a human's text for any reason beyond checking TODO items, say what you changed in a separate comment that carries the footer.

## Branches and Pushes

- For tasks whose changes are to be committed, commit early and often and push checkpoints to an authorised branch, whether you work in a worktree or a harness-provided checkout.
- Never commit to or push `main`. Changes reach it only through PRs.
- Merge a PR into `main` only after explicit human approval.
- Never push a branch you did not create unless a human asks you to. A branch that your harness explicitly designates as the push or delivery target counts as asked for. Merely checking out a branch or naming it as the place to work does not authorise publishing it; verify the intended remote and target branch before pushing.
- Force-push, commit amendment, destructive reset or checkout, rewriting another person's published history, tags, and releases require explicit task authorisation.
