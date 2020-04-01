# Prism

Prism is a Self-Sovereign Identity (SSI), and Verifiable Credentials (VC) platform.

[![CircleCI](https://circleci.com/gh/input-output-hk/atala/tree/develop.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)

## Branches

Two main branches will be maintained: `develop` and `master`. `master` contains the latest version of the code that was tested end-to-end. `develop` contains the latest version of the code that runs all the tests (unit and integration tests). Integration tests don't test all the integrations. Hence, any version in `develop` might have bugs when deployed for an end-to-end test.


## Working with the codebase

This is a monorepo and each of the `credentials-verification-XYZ` folders refers to a different part of the platform. Check the specific READMEs for more details.

Be sure to follow our [contributing guidelines](CONTRIBUTING.md).

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):

- Install [coursier](https://github.com/coursier/coursier#command-line), the `coursier` command must work.
- `./install-scalafmt.sh` (might require sudo).
- `cp pre-commit .git/hooks/pre-commit`

## More docs

* Documentation about operational aspects of the team and the services we use can be found in [Confluence](https://input-output.atlassian.net/wiki/spaces/CE/pages/606371843/Code+and+Infrastructure+Setup).
* The general guideline and ultimate goal is to have the [repository](credentials-verification/docs/README.md) as the source of truth for all technical documentation.
