# Contributing
These are the Atala PRISM contributing guidelines, which apply to all our repositories.


## Branches
Atala PRISM intends to two working versions:
- The latest version, which has the latest state of the code, `master` right now.
- The long-term supported version (LTS), like `1.3.x`, which we maintain by applying critical bugfixes.

Most Pull Requests (PRs) will be sent to `master`, critical bugfixes will be backported to the current LTS version.


### Naming

Branch names use this format `ATA-XXXX-short-feature-name` where:

- `ATA-XXXX` is the Jira ticket related to the branch.
- `short-feature-name` is a short name for the ticket (used to get an idea about the branch without opening Jira).


### Special branches

Besides `master`, there are some special branches, which are in a code-freeze, meaning that they will only accept changes that are strictly necessary, pushing code to these branches is restricted to Technical Architects:

- `www` has the version running in production (found at `https://www.atalaprism.io` and `https://atalaprism.io`), at the moment, production is the interactive demo connected to the mobile apps from PlayStore/AppStore.
- `1.3.x` has the code for `1.3.x`, at the moment, runs the code use by the people involved in the Prism Pioneer Program (PPP).



## Commits
All commits must be signed and verified, otherwise, the Pull Request won't be merged, for that you will need to set up PGP for git (check [More](#More) for the revelant docs). These rules are enforced on these branches:
- master
- ATA* (`ATA` prefix on the branch name).

Every commit must follow these rules:
- Must be a small incremental change.
- Must have a clear message.
- Every merged commit to `master` or special branches must succeed on the CI, keeping backwards compatibility.

When backwards compatibility is not possible, use feature flags so that the new feature could be enabled when needed instead of breaking all the clients.

Ignore the backwards compatibility rule when it isn't practical, but this must have approval from the team.


## Pull Requests and bug fixes

Each pull request should be small enough to be reviewed in less than 30 minutes, otherwise, break the PR into several ones, or use several self-contained commits.

**NOTE**: It's important to create new commits when addressing the reviewer comments (instead of `git commit --amend`), so that we know what exactly changed, when the PR is approved, squash your commits or reorganize them to remove the dirty commits.


### Pull Request base branch

Most times, PRs will be sent to `master`, in the case of a bug fix, it must be submitted to the branch where the bug was reported (back-porting to special branches when necessary), for example:

- When a bug is reported for `1.3.x`, we'll submit a PR to `1.3.x` and `master`.
- When a bug is reported for `master`, we'll submit a PR to fix `master`, if we detect that the bugfix is required by any special branch, we'll back-port the fix to such special branch.

### Pull Request naming

Pull Requests are expected to me named like `ATA-XXXX: A short description` (ticket id as prefix with a short but understandable description).

### Pull Request checklist

Most Pull Requests will get a checklist that everyone is expected to follow, failing to do so might delay people getting to review it.

### Pull Request review

The team `input-output-hk/atala` will be automatically set as reviewer when the PR is created, so that 2 reviewers from the team get assigned. After that, you can add any other reviewer that should review the PR.

At least 1 approval is required to merge a PR.

### Pull Request merging

Avoid merges, use rebase instead, a simple way is to just run `git config --global pull.rebase true` on each repository or run `git config --global pull.rebase true` once to get the effect on all repositories (check the [More](#More) section for simple ways to set this up and forget about it).

Once the Pull Request is ready to be merged, you will be required to use the `Squash and Merge` feature from Github, which combines all the PR commits into a single one, you are expected to review the auto-generated message and update it as necessary to get a decent commit message.

**NOTE** If a PR breaks `master`, we shouldn't merge it but keep it opened until we are able to merge it without breaking `master`, this applies to experimental or controversial changes.
NOT merge experimental stuff, keep it in a PR

You are responsible to merge your PRs when they are ready, before that, ensure that your branch has all the changes from the base branch (like `master`).


## Releases
When we are ready to release a new LTS version, we need to follow this process, be sure to update the necessary placeholders.

**NOTE** Our plan is to keep a single LTS version, hence, `master` becomes the `LTS` version, the previous `LTS` version is drop.


### Assumptions

1. Our release is called `Core DID`, this name covers all the components that are being released together (even if each component has a different release name).
1. Our next version for the SDK/Node is `1.3.0`.
1. `master` branch CI build passed, having any neccessary artifact published/deployed.


### Per-repository process

The process applies to all repositories involved in a release (SDK/Node in this example):

1. Create a new release candidate (rc) branch based on `master` called `1.3.x`.
1. Tag the latest commit from `1.3.x` as `v1.3.0-rc1` (increase the `rc` suffix if `v1.3.0-rc1` already exists).
1. Coordinate with our internal DevOps team to make sure `1.3.x` gets deployed to a new environment.
1. Share the tag and environment with our QA team, who will execute their acceptance tests.
1. If the is any problem, let's fix it, submitting a PR to `1.3.x` as well as submitting it to `master`, then go to step 2.
1. Prepare the release notes and create a realease in Github with those for `v1.3.0-rc1`.


### Final process
Once all repositories are released:

1. Share the latest `rc` tags for each project to our internal DevOps team, which will release `1.3.x` to the environment intended for our consumers (like `ppp.atalaprism.io`).
1. Share the new environment to our QA team, who will execute their acceptance tests.
1. If anything went wrong, go back to the [Per-repository process](#Per-repository process) increasing the `rc` number.
1. Collect the relevant release notes for a public facing release.
1. Draft the official release notes, then, share those with our Technical Writer to polish them.
1. Create a tag in each repository with the released version, `v1.3.0` in this case.
1. Update the [atala-releases](https://github.com/input-output-hk/atala-releases) repository include the new released version details (`Core DID`), be sure to link to artifacts related to `v1.3.0` instead of the release candidates.
1. Celebrate. 


## More
- See [singing-commits.md](./signing-commits.md)
- See [cli-settings.md](./cli-settings.md)
