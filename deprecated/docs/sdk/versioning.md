# SDK Versioning
This document describes Atala's approach to SDK versioning and its further evolution. Although some things that are going to be said in this document can be applied to all supported platforms, we are going to talk about Kotlin/JVM specifically.

## Context
Atix/PSG are going to start the development of MoE services very soon. Naturally, they will be using the PRISM SDK, and they need some guarantees of SDK's stability. Precisely, backwards compatibility should be kept for the length of the first stage of the project (~1.5 years).

## Goal
The goal of this documents is threefold:
1. Define PRISM SDK's versioning scheme and what kind of backwards compatibility guarantees we provide between them depending on the version evolution.
2. Provide some practical guidance on how to help us enforce these guarantees.
3. Define where we are going to store the artifacts and how we are going to disseminate them.

## Versioning Scheme
We propose to use [semantic versioning](https://semver.org/). This means that each PRISM SDK's release version will look like MAJOR.MINOR.PATCH where incrementing:
- MAJOR version means that non-backward compatible changes were introduced
- MINOR version means that new functionality was added in backward compatible manner
- PATCH version means that backward compatible bug fixes or security patches were applied

Note that we will be using single MAJOR version of PRISM SDK during the entire first stage of MoE project.

### Deprecation
Deprecating existing functionality is a normal part of any development process. As such, MINOR releases can mark classes/methods as deprecated (without deleting them). Furthermore, we shall guarantee that there will be at least one MINOR release that deprecates a functionality before a MAJOR release that completely removes it.

### Release Notes
We propose to include the following into each version's release notes:
- MAJOR releases will come with (relative to the last MAJOR release):
    - General overview of the release and extensive human-readable changelog
    - A full migration guide on removed/changed functionality
- MINOR releases will come with (relative to the last MINOR release in the given MAJOR release):
    - A list of all deprecated classes/methods along with our suggestions on how to do the supported flows without them
    - A list of newly introduced classes/methods along with their proposed usages
- PATCH releases will come out with a small changelist outlining the fixed bugs

Release notes will be kept in CHANGELOG.md, the format of which will be based [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). Each new version's changelog will be added as a new entry to CHANELOG.md, timestamped by the UTC release date. Additionally, each new version will be accompanied with a GitHub tag that will contain a link to the relevant part of CHANGELOG.md.

As the release can affect multiple platforms at once, the release note can have tags dedicated to each particular platform. For example, common API changes can have no tag at all, JVM changes can have "[JVM]" and JS changes "[JS]".

See [versioning example](versioning-example.md) for reference.

### Documentation
We propose to maintain one documentation instance for the latest MINOR.PATCH release in each MAJOR branch (similarly to how [http4s](https://http4s.org/) does it). This will mean adapting our documentation website generation engine to support multi-version layout, design of which is out of this document's scope.

### Transitive dependencies
We need to carefully review all SDK's dependencies and mark them as `implementation` or `api` accordingly. This will make sure that only relevant dependencies are exposed to end-users transitively. For example, it makes sense to expose `pbandk` to provide protobuf primitives to the user, but it would not make sense to expose `bitcoin-kmp` along with our own cryptographic primitives.

Marking dependencies is important as it will affect our ability to upgrade some dependencies in a given MAJOR branch. It is especially important in case of `pbandk` which auto-generates our protobuf models and gRPC services.

## Practical Enforcements
First, we need to make sure that nothing is exposed to the users unintentionally. Modern languages (such as Kotlin) have moved away from the original Java's visibility approach where you have to specify visibility modifiers explicitly if you want to expose the entity to users. This makes it easier to write your everyday backend code, but actually makes the life harder for library authors. Fortunately, Kotlin 1.4 introduced a new feature called [explicit API mode](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors). We propose to enable it for all SDK modules in strict mode.

Second, after we have reviewed and frozen our public API, we need to make sure that the API does not change unintentionally. For this we propose to use [Kotlin binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator): a gradle plugin that can dump the binary API into a file and make sure that it is not affected by proposed changes.

## Interoperability with Java
As a part of our guarantee, we will also offer Java interoperability along with the Kotlin/JVM one. Practically, this means properly annotated static fields/methods (with `@JvmField`/`@JvmStatic`) and package-level functions.

## Experimental API
In the future we might offer experimental features that would not fall under our backward compatibility guarantees. Such API will be marked with a special [opt-in annotation](https://kotlinlang.org/docs/opt-in-requirements.html) that would give users a warning about this being an experimental feature.

## Artifactory
We will be producing Apache Maven artifacts that would need to be published on Apache Maven registry. [Github Packages](https://github.com/features/packages) offers [such a registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry). So it seems like a reasonable choice given that the artifacts are only meant to be consumed by internal IOHK developers and Github Packages provide organization-level privacy.
