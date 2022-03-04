# Contributing
These are the Atala PRISM contributing guidelines, which apply to all our repositories.


## Branches
Atala PRISM commits to two working versions:
- The latest version, which has the latest state of the code, `master` right now.
- The long-term supported version (LTS), like `1.3.0` or `1.4.0` etc..., which we maintain by applying critical bugfixes.

Most Pull Requests (PRs) will be sent to `master`, critical bugfixes will be back-ported to the current LTS version.


### Naming

Branch names use this format `ATA-XXXX-short-feature-name` where:

- `ATA-XXXX` is the Jira ticket related to the branch.
- `short-feature-name` is a short name for the ticket (used to get an idea about the branch without opening Jira).


### Special branches

Besides `master`, there are some special branches, which are in a code-freeze, meaning that they will only accept changes that are strictly necessary, pushing code to these branches is restricted to Technical Architects:

- `www` has the version running in production (found at `https://www.atalaprism.io` and `https://atalaprism.io`), at the moment, production is the interactive demo connected to the mobile apps from PlayStore/AppStore.
- Branch that has the long term supported version, such as `1.3.0` or `1.3.1` or whatever the latest release branch is.



## Commits
All commits must be signed and verified, otherwise, the Pull Request won't be merged, for that you will need to set up PGP for git (check [More](#More) for the relevant docs). These rules are enforced on these branches:
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

- When a bug is reported for `1.3.1`, we'll submit a PR to `1.3.1` and `master`. There will always be only one LTS version, which is the latest release
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

**NOTE** Our plan is to keep a single LTS version, hence, `master` becomes the LTS version, the previous LTS version is dropped.


### Assumptions

1. Our next release is called `Core DID`, this name covers all the components that are being released together (even if each component has a different release name).
1. Our next version for the SDK/Node is `1.4.0`.
1. `master` branch CI build passed, having any necessary artifact published/deployed.


### Per-repository process

The process applies to all repositories involved in a release:

1. Create a new release candidate (rc) branch based on `master` called `release/<release version number>` for example, if we are planning to release 1.4.0, branch will be `release/1.4.0`
2. Tag the latest commit from that branch as `v<release version number>-rc1`. Increase the `rc` suffix if `rc1` already exists. For example if `v1.4.0-rc1` tag already exists, create a tag `v1.4.0-rc2`
3. Coordinate with our internal DevOps team to make sure code from the latest rc tag is deployed to environment for QA to test it (for SDK, deploy manually. Check [below](#SDK))
4. Check the commit diff between upcoming release and latest release, and make sure that upcoming release commits only include tickets (alongside with maintenance PRs such as library updates, etc...) intended for this release
   1. If that is not the case, and upcoming release has tickets that have been intended for future releases, move this tickets from future release in jira to the current one.
   2. **Note:** this usually does not happen, but exceptions exist and we need to make sure that all tickets that are included in the release candidate are listed as tickets in jira, QA depends on it.
5. Share the deployed environment to QA team to test it (in case of sdk, share published sdk version for testing)
6. If QA finds an issue, introduce a PR fix to master, backport the fix to release branch, tag the latest commit with new tag, in this case `v<release version number>-rc2`, go to step 3.
7. Repeat this process (step 1 to 6 except step 4, it only needs to be done once) until QA approves the release candidate
8. From the latest commit create a release tag `v<release version number>`, in the case of example above, tag name would be `v1.4.0` 
9. Create a release from this tag in github, make sure to include release notes in release description. Notes should be derived from jira tickets of this release. Consider getting a help from our technical writer if release is major, with lots of changes and you need to polish them
10. Coordinate with our internal devops team to release the code from release tag (`v1.4.0`) to production, this version will now become the LTS, previous LTS will be dropped.
11. Update the [atala-releases](https://github.com/input-output-hk/atala-releases), repository include the new released version details (`Core DID` for example), be sure to link to artifacts related to `v1.4.0` instead of the release candidates.
12. 🎉

#### SDK

sdk with a custom name needs to be published manually from the shell, run this inside sdk repo root:
```bash
PRISM_VERSION=<verson mame to publish> ./gradlew publishAllPublicationsToGitHubPackagesRepository
```
if you are publishing v1.4.0-rc1 version of sdk for example, use `PRISM_VERSION=v1.4.0-rc1`
this assumes you have environment variables `GITHUB_TOKEN` and `GITHUB_ACTOR` defined in your shell profile, if not run

```bash
PRISM_VERSION=<verson mame to publish> GITHUB_TOKEN=<token> GITHUB_ACTOR=<your github username>  ./gradlew publishAllPublicationsToGitHubPackagesRepository

```
**NOTE:**
* token must have `write:packages` access in order to be able to publish a package
* Github access tokens are generated [here](https://github.com/settings/tokens)
* `GITHUB_ACTOR` should have access to sdk package


## More
- See [singing-commits.md](./signing-commits.md)
- See [cli-settings.md](./cli-settings.md)
