
# Configuring Git
There are some tricks used by the team to set up the repository config easily, feel free to use them (make sure to use your own data) or share yours.

## A bash script
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


## A bash function
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

## Directory specific settings

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
