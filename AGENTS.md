# AI Agent Instructions

Read the sibling [`README.md`](README.md) and the `README.md` of every directory you work in before you start. They are as binding as this file. The AI harness should inject relevant `AGENTS.md`s automatically, but not READMEs. `AGENT.md`s hold only agent-specific rules; everything humans need, too, lives in the READMEs.

## Worktrees

If you work on a task on your own, you must do so in your own worktree. This does not apply when you’re called in to help or pair program in existing work.

When applicable, every agent task shall use its own branch and its own worktree. Never share a worktree with another task or a human; taking over the worktree of a stopped agent is the one exception.

1. Fetch, then create the worktree under `../consistent.software-worktrees/<branch>` relative to the root checkout (this location survives reboots; create the directory if it is missing):

    ```sh
    git fetch origin
    mkdir -p ../consistent.software-worktrees/
    git worktree add -b <branch> ../consistent.software-worktrees/<branch> <base>
    ```
  `<base>` is usually `refs/remotes/origin/main`. Use another base only if the task needs it.

2. Enter the worktree and verify `pwd`, `git branch --show-current`, and `git status `.
3. Set your commit identity (see [Commit Attribution](#commit-attribution)).
4. If `.codegraph/` is absent, run `codegraph init`. This is authorized; the cache is gitignored and must never be committed.
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

## Branches and Pushes

- Never commit to or push `main`. Changes reach it only through PRs.
- Merge a PR into `main` only after explicit human approval.
- Never push a branch you did not create unless a human asks you to. Exception: when you are assigned to a service tracker, you may push the branches listed in its `## Branches` section.
- Force-push, commit amendment, destructive reset or checkout, rewriting another person's published history, tags, and releases require explicit task authorization
