# Contributing
These are the Atala PRISM contributing guidelines, which apply to all our repositories.


## Branches
Atala PRISM intends to have two working versions:
- The latest version, which includes everything, `master` right now.
- The long-term supported version (LTS), like `1-3-x`, which we maintain by applying critical bugfixes.

Most Pull Requests (PRs) will be sent to `master`, critical bugfixes will be backported to the LTS version.


### Naming

Branch names use this format `ATA-XXXX-short-feature-name` where:

- `ATA-XXXX` is the Jira ticket related to the branch.
- `short-feature-name` is a short name for the ticket (used to get an idea about the branch without opening Jira).


### Special branches

Besides `master`, there are some special branches, which are in a code-freeze, meaning that they will only accept changes that are strictly necessary, pushing code to these branches is restricted to Technical Architects:

- `www` has the version running in production (found at `https://www.atalaprism.io` and `https://atalaprism.io`), at the moment, production is the interactive demo connected to the mobile apps from PlayStore/AppStore.
- `1-3-x` has the code for `1.3.x`, at the moment, runs the code use by the people involved in the Prism Pioneer Program (PPP).


## Pull Requests and bug fixes

Most times, PRs will be sent to `master`, in the case of a bug fix, it must be submitted to the branch where the bug was reported (back-porting to special branches when necessary), for example:

- When a bug is reported for `1.3.x`, we'll submit a PR to `1-3-x` and `master`.
- When a bug is reported for `master`, we'll submit a PR to fix `master`, if we detect that the bugfix is required by any special branch, we'll back-port the fix to such special branch.

### Pull Request naming

Pull Requests are expected to me named like `ATA-XXXX: A short description` (ticket id as prefix with a short but understandable description).

### Pull Request checklist

Most Pull Requests will get a checklist that everyone is expected to follow, failing to do so might delay people getting to review it.

### Pull Request merging

Once the Pull Request is ready to be merged, you will be required to use the `Squash and Merge` feature from Github, which combines all the PR commits into a single one, you are expected to review the auto-generated message and update it as necessary to get a decent commit message.


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


## Releases
When we are ready to release a new LTS version, we should:

1. Create a branch, like `1-3-x` (fix required to use `1.3.x` instead).
1. QA will run their acceptance tests, we should fix anything that QA require us to fix.
1. We'll create a tag, like `v1.3.0`.
1. We'll create a github release with proper release notes.
1. We'll update the [atala-releases](https://github.com/input-output-hk/atala-releases) repository to include the new released version details.

## More
- See [singing-commits.md](./signing-commits.md)
- See [cli-settings.md](./cli-settings.md)
