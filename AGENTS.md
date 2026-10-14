# AI Agent Instructions

Before you start, read [`contributing/INDEX.md`](contributing/INDEX.md). It tells you which document contains what. Whenever information from one of these documents is relevant to your task, look it up there. This includes the `README.md` (how to use a directory) and `ARCHITECTURE.md` (how a directory works and how to maintain it) of the directories you work in. These documents are as binding as this file. The AI harness should inject relevant `AGENTS.md` files automatically, but not these documents. `AGENTS.md` files hold only agent-specific rules; everything humans need, too, lives in the documentation for humans.

## Worktree vs In-Project Changes

Before editing any files, judge whether your task should be done in a separate worktree or in the current folder. Here is some guidance:

- If your task starts at a clean commit state and shall be committed, you most likely should use your own worktree.
- If your task makes sense only in the context of the current, dirty, project folder state and there is no other indication for using a worktree, you should likely edit the current folder.

If you are unsure, ask the user for clarification on whether to use a worktree or the current folder.

## Worktrees

When using a worktree, every agent task shall use its own branch and its own worktree. Never share a worktree with another task or a human; taking over the worktree of a stopped agent is the one exception.

1. Fetch, then create the worktree under `../consistent.software-worktrees/<branch>` relative to the root checkout (this location survives reboots; create the directory if it is missing):

   ```sh
   git fetch origin
   mkdir -p ../consistent.software-worktrees/
   git worktree add -b <branch> ../consistent.software-worktrees/<branch> <base>
   ```

   `<base>` is usually `refs/remotes/origin/main`. Use another base only if the task needs it.

2. Enter the worktree and verify `pwd`, `git branch --show-current`, and `git status`.
3. Set your commit identity (see [Commit Attribution](#commit-attribution)).
4. If `.codegraph/` is absent, run `codegraph init`. This is authorised; the cache is gitignored and must never be committed.
5. Do all work from the worktree. Commit early and often and push checkpoints of your own branch.
6. Keep branches and worktrees short-lived. Clean up only after merge or explicit abandonment, and only when `git status --porcelain` prints nothing: remove `.codegraph/`, then run `git worktree remove <path>` from another checkout.

## Commit Attribution

Commits that contain only agent work carry the agent's identity. This is the standard case. Right after creating the worktree, run:

```sh
git config --worktree user.name "Agent Name"
git config --worktree user.email "agent-name@noreply"
git config --worktree commit.gpgsign false
```

The email must be a valid address: lowercase, spaces become hyphens, for example `Sisyphus Junior <sisyphus-junior@noreply>`. Signing is disabled so that no human's key signs agent commits.

Only when a human and an agent both wrote content in the same commit, keep the human's identity and add the trailer `Co-authored-by: Agent Name <agent-name@noreply>`.

`--worktree` requires `extensions.worktreeConfig`. If Git rejects the command, stop and ask a maintainer; never write these settings to repository-wide or global configuration.

## GitHub

- Act on issues and PRs only as far as the task requires. Do not create unrelated issues, PRs, or releases.
- Apart from commit authorship, you act on GitHub under the identity of the human who launched you.
- Because GitHub shows that human as the author, end everything you write there with this footer: PR titles excluded, but PR descriptions, issues, comments, review comments, and discussion posts included. Use the same agent name as in your commit identity, and put a blank line before the footer so that it forms its own paragraph:

  ```markdown
  <sub>Written by the AI agent Agent Name.</sub>
  ```

  When you edit something you posted earlier, keep the footer. Don't add it to text a human wrote. If you change a human's text for any reason beyond checking TODO items, say what you changed in a separate comment that carries the footer.

## Branches and Pushes

- Never commit to or push `main`. Changes reach it only through PRs.
- Merge a PR into `main` only after explicit human approval.
- Never push a branch you did not create unless a human asks you to. Exception: when you are assigned to a service tracker, you may push the branches listed in its `## Branches` section.
- Force-push, commit amendment, destructive reset or checkout, rewriting another person's published history, tags, and releases require explicit task authorisation.
