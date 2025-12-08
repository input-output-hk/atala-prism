# CI/CD Workflows Overview

This project uses GitHub Actions for formatting, testing (unit + E2E), linting, multi-arch Docker image builds, and releases. Below is a concise reference of each workflow, triggers, and key steps.

## Build & Test (`.github/workflows/ci.yml`)
- **Triggers:** push to `main` and tags `v*`; pull requests.
- **Jobs:**
  - **Format & Unit Tests:** checkout → setup Scala (Java 11) → cache sbt/coursier → `sbt scalafmtCheckAll` → `sbt coverage test` → coverage report/aggregate → publish test results.
  - **E2E Tests:** depends on unit job → checkout → setup Scala + cache → `sbt "Docker / publishLocal"` to ensure the prism-node image exists → `./docker/prism-test/run-e2e.sh` (brings up compose stack and runs gRPC VDR E2E specs).
- **TLS overrides:** `JAVA_TOOL_OPTIONS`, `SBT_OPTS`, `COURSIER_JAVA_ARGS` enforce TLS 1.2 for dependency resolution.

## Docker Publish (`.github/workflows/docker-publish.yml`)
- **Triggers:** push to `main`, push to tags `v*`, manual dispatch.
- **Steps:** checkout → setup Scala + cache → `sbt "Docker / stage"` to produce the native-packager context → Docker metadata (semver on tags, sha, edge per branch/main, latest on release tags) → Docker Hub login → QEMU + Buildx multi-arch → build/push from `target/docker/stage` for `linux/amd64,linux/arm64`.
- **Image:** `docker.io/inputoutput/prism-node` with tags from metadata-action.

## Linting
- **Mega-Linter (`.github/workflows/megalinter.yml`):** runs Mega-Linter (v7.13.0) on PRs. Settings in `.mega-linter.yml` disable some noisy scanners, exclude submodules, and apply TLS overrides via env. Artifacts: `megalinter-reports` and `mega-linter.log`.
- **PR title lint (`.github/workflows/pr-lint.yml`):** checks PR titles against conventional commits (if configured).

## Release (`.github/workflows/release.yml`)
- Uses `semantic-release` to cut releases. Steps include GHCR login, Helm/Node setup, GPG import, and `semantic-release`.
- **Semantic release config (`package.json`):**
  - Branches: `main`, `prerelease/*` (prerelease labeled `snapshot`).
  - Tag format: `v${version}`.
  - Plugins/execs: bump npm version (no git tag), run `sbt "release release-version ..."` for Scala versioning, build/push multi-arch image to GHCR from `target/docker/stage`, bump Helm chart (`Chart.yaml`, index, package), changelog, git assets commit, Slack notifications.

## Local Tips
- Unit/format: `sbt scalafmtCheckAll && sbt coverage test && sbt coverageReport coverageAggregate`.
- E2E: `sbt "Docker / publishLocal"` then `./docker/prism-test/run-e2e.sh` (starts compose and runs gRPC tests).
- Docker image from sbt: `sbt "Docker / stage"` then `docker buildx build target/docker/stage` (set desired tags and platforms, e.g., `--platform=linux/amd64,linux/arm64`).
