# Contributing to consistent.software

First off, thanks for taking the time to contribute! All types of contributions are encouraged and valued.

## Commit & PR message format

We use [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) on `main` and we [squash](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/configuring-commit-squashing-for-pull-requests) pull requests.
That means:

1.  Your PR title must summarise the change and be formatted according to [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/). The allowed values for `type` [are defined by `commitizen/conventional-commit-types`](https://github.com/commitizen/conventional-commit-types/blob/master/index.json).
2.  Your PR description will be included in the final, squashed commits. It should contain information on design decisions, trade-offs, and implementation details.
3.  Individual commits of a PR are not required to follow conventional commits, although we still recommend it. It is okay to use the conventional commit `type` relative to your branch.
    For example, use the type `fix` to fix a bug you introduced on the same branch, even though that bug was never on `main`.
