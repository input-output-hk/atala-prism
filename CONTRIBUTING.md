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

## Setting up PGP

Every commit must be signed with a PGP key associated with your company email address, that is `firstname.lastname@iohk.io`. For IOHK employees it **must** be the same key you use to encrypt IOHK emails, it can not be your personal PGP key or any other one. 

### Install gnupg

#### Linux
```bash
sudo apt-get install gnupg
```
#### Mac
```bash
brew install gnupg
```
#### Windows
Download and install gnupg for windows from [GnuPG website](https://gnupg.org/download/index.html)

### Generating a key

In case you've already generated your company PGP key pair before, you need to import a private key
```bash
gpg --import private.key
```
If you have not generated your company PGP key pair yet
```bash
gpg --full-generate-key
```
 and follow the instructions, make sure to associate this key with your company email address. For key size, choose 4096

### Using the key

#### Local setup
Set up git to automatically sign every commit with your company PGP key, first get your GPG key id
```bash
gpg --list-keys
```
will list all the keys you have available. copy the id of the key associated with your company email address.

configure git to use this key to sign every commit automatically
```bash
git config user.signingkey <your key id here> && 
git config commit.gpgsign true
# in case you prefer to use another tool for pgp, like gpg2, you need to specify it here, otherwise ignore it.
git config gpg.program gpg2
```
Check the [Configuring Git](#Configuring-Git) section for simple ways to set this up and forget about it.

#### Remote setup

You need to add the public key to Github, so that it can verify commits signed by the associated private key.

export your public key
```bash
gpg --armor --export firstname.lastname@iohk.io
```
This will output the key into your terminal. Copy the whole key (including -----BEGIN PGP PUBLIC KEY BLOCK----- and -----END PGP PUBLIC KEY BLOCK----- part) and add it [into your account](https://github.com/settings/keys)

*NOTE:* Make sure to add your company email address into [your github account emails](https://github.com/settings/emails) and confirm it. Github will allow you to add public keys associated with any email, but if this email is not added into your emails, it assumes that you are not the owner of this email address, and even if commits are signed with a proper private key, they will not be verified.

#### Troubleshooting
in case commiting a change fails with the message
```bash
error: gpg failed to sign the data
fatal: failed to write commit object
```
try the following
```bash
gpgconf --kill gpg-agent
export GPG_TTY=$(tty)
echo "test" | gpg --clearsign
```
if this problem keeps happening, try adding `export GPG_TTY=$(tty)` into your `~/.bashrc` or `~/.zshrc` file.

## Configuring Git
There are some tricks used by the team to set up the repository config easily, feel free to use them (make sure to use your own data) or share yours.

### A bash script
Create the `~/scripts/setup-iohk-repo.sh` file:

```bash
#!/bin/bash
git config user.name "Firstname Lastname"
git config user.email "firstname.lastname@iohk.io"
git config user.signingkey <your key id here>
git config commit.gpgsign true
git config pull.rebase true
```

Every time you clone an IOHK repository, move into the folder and run `~/scripts/setup-iohk-repo.sh` (or create a custom clone script that does both steps).

This script will configure your local `.git/config` file.


### A bash function
Edit the `~/.bashrc` or `~/.zshrc`, and add the following:
```bash
function gitconfigiohk {
  git config user.name "Firstname Lastname"
  git config user.email "firstname.lastname@iohk.io"
  git config user.signingkey <your key id here>
  git config commit.gpgsign true
  git config pull.rebase true
}
```

Every time you clone an IOHK repository, move into the folder and invoke `gitconfigiohk` (or create a custom clone script that does both steps).

This script will configure your local `.git/config` file.

### Folder specific settings

If you prefer to store your IOHK specific repositories in a separate folder, you can specify gitconfig for this repository only.

**at the end** of your global .`gitconfig` file add

```
[includeIf "gitdir:~/code/iohk/"] // path to the folder where all iohk repos are cloned
    path = ~/code/iohk/.gitconfig-iohk // path to the iohk specific gitconfig file
```
check [Git conditional includes](https://git-scm.com/docs/git-config#_conditional_includes) for more.

Add this to your `.gitconfig-iohk`

```
[user]
	name = Firstname Lastname
	email = firstname.lastname@iohk.io
    signingkey = <your key id here>
[commit]
	gpgsign = true
[pull]
	rebase = true
```
