# Contributing
This document explain the rules for contributing to the project.


## Branches
Each feature branch must have the ticket id as prefix, which is usually `ATA-XXXX`, for example, the `ATA-9999-update-docs` branch is related to the ticket `ATA-9999`.


## Commits
All commits must be signed and verified, otherwise, the Pull Request won't be merged, for that you will need to set up PGP for git, check [Setting up PGP](#Setting-up-PGP). These rules are enforced on these branches:
- master
- develop
- ATA* (`ATA` prefix on the branch name).

Every commit must follow these rules:
- Must be a small incremental change.
- Must have a clear message.
- Every merged commit on `develop` and `master` branches must succeed on CircleCi and should keep backwards compatibility.

When backwards compatibility is not possible, use feature flags so that the new feature could be enabled when needed instead of breaking all the clients.

Ignore the backwards compatibility rule when it isn't practical, but this must have approval from the team.


## Pull requests
Each pull request should be small enough to be reviewed in less than 30 minutes, otherwise, break the PR into several ones or use several self-contained commits.

A pull request must have all checks succeeding before being able to merge, and at least one approval is required before merging the PR.

The team `input-output-hk/atala` will be automatically set as reviewer when the PR is created, so that 2 reviewers from the team get assigned. After that, you can add any other reviewer that should review the PR.

**NOTE**: It's important to create new commits when addressing the reviewer comments (instead of `git commit --amend`), so that we know what exactly changed, when the PR is approved, squash your commits or reorganize them to remove the dirty commits.

You are responsible to merge your PRs when they are ready, before that, ensure that your branch has all the changes from the base branch (like `develop`).



## Merging
Avoid merges, use rebase instead, a simple way is to just run `git config --global pull.rebase true` on each repository or run `git config --global pull.rebase true` once to get the effect on all repositories. Check the [Configuring Git](#Configuring-Git) section for simple ways to set this up and forget about it

The reasons to follow this approach are:
- The git history won't get polluted, we have saw the pain that this causes when dealing with huge pull requests.
- `git bisect` and `git cherry-pick` will actually work.
- It's easy to rewrite history, which is sometimes more complex when it contains merge commits.

And the drawbacks:
- A little bit more work for the developer.
- Every time you rebase and do a force push, the commits will change, hence, existing comments on the PR will point to dead links, due to this, avoid rebasing until your PR gets approved, or you are requested to rebase. or you need new changes from the upstream in your code.

The team consensus is that the advantages greatly outweigh the disadvantages.


## More
- See [singing-commits.md](./signing-commits.md)
- See [cli-settings.md](./cli-settings.md)
