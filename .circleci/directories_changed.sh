#!/bin/bash

# This script detects whether at least one of the directories given as arguments have changed and exits with a non-zero
# code in that case, or zero if nothing has changed. If the current branch should always be tested, this script will
# always exit with a non-zero code, independently of directory changes.
# This script is used by CircleCI (.circle/config.yml), in order to detect whether a component or its dependencies have
# changed and test it.
# $CIRCLE_* variables should always be set in CircleCI, the below checks for emptiness are to aid local testing, but
# running `CIRCLE_BRANCH=develop ./.circleci/directories_changed.sh`, and alike, can be used as well.

# Precautionary measure: if the script fails, testing should be performed.
set -euo pipefail

echo "Detecting if any of directories '$*' changed"

branch=${CIRCLE_BRANCH:-}
if [ -z "$branch" ] ; then
  # Use head branch
  branch=$(git rev-parse --abbrev-ref HEAD)
fi

echo "Current branch is '$branch'"

# Branches that deploy an image should be fully tested
ALWAYS_TEST_BRANCH_REGEX="^(develop|test|geud-test|demo)"
if [[ $branch =~ $ALWAYS_TEST_BRANCH_REGEX ]] ; then
    echo "Branch '$branch' should always be tested"
    exit 1
fi

current_commit=${CIRCLE_SHA1:-}
if [ -z "$current_commit" ] ; then
  # Use head commit
  current_commit=$(git rev-parse HEAD)
fi

# Detect changed files, ignoring doc files (.md and everything inside a docs directory) and other files not affecting
# builds or tests
# "git diff A...B" is equivalent to "git diff $(git merge-base A B) B" (source: git help diff)
set +e # Ignore grep returning an exit code of 1 when no line is selected
changed_files=$(
  git diff origin/develop..."$current_commit" --name-only | \
      grep -v "\.md$" | \
      grep -v "/docs/" | \
      grep -v "\.gitignore$")
set -e # Re-enable check

echo "Changed files:
$changed_files"

for dir in "$@" ; do
  # Matches a line starting with the directory name, followed by a slash
  dir_regex="(^|
)$dir/"
  if [[ $changed_files =~ $dir_regex ]] ; then
    echo "Directory '$dir' has changed"
    exit 1
  fi
done

echo "None of directories '$*' has changed"
