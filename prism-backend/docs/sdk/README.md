# SDK
This small doc aims to specify the approach we plan to take to build a decent one.


## Goals
- Expose the necessary functionality we already use on our different platforms.
- Use a modular design, so that only the necessary pieces get integrated while building a project.
- Support mainly Scala/JavaScript/Android/Java.
- Include anything added to Mirror that gets abstract enough if we have the time.
- Compile to a single Scala version (the one being used at the time in our projects).
- Make sure to support anything required by the Georgia deployment.
- Type checked docs (https://scalameta.org/mdoc/).
- Docs to cover the main end to end flows (issue/verify/revoke a credential).
- Build it as if the SDK was already open source.

## Non-goals
- Add new features that aren't properly justified.
- Support iOS, even that most of the stuff is there, no core member is familiar enough with Swift to invest there now.
- Idiomatic JavaScript/Java docs.
- Prism Wallet SDK, it's not mature enough, and there isn't much we can do with it, we'd also need more tooling related to JavaScript (which needs research).

## Misc stuff to consider before a proper release:
- Update overall documentation, architecture, diagrams, etc
- Analyze publishing js libraries to npm/github, if so, publish automatically on each release.
- Publish the libraries automatically to maven central (https://github.com/olafurpg/sbt-ci-release).
- Document how to use the APIs to run end to end flows in every supported language.
- Document how to generate, and recover a wallet.
- Document how to recover data from the encrypted data vault (to be defined)


## Modules
The suggested approach for its simplicity is to create a root level directory named `sdk`, containing a separate directory per sdk module. Every module would be a cross-compiled sbt project using git-based versioning.

Note that while we have discussed the prism-node library, it doesn't provide much value besides the auto-generated client, hence, its excluded for now.

Also, the SDK requires Android/Java bindings for all modules.


### prism-crypto
This is the lowest level module, the library scope being only the crypto primitives to power the system (signatures, encryption, hashing, etc), this module has heavy usage from Android, hence, Java/Android bindings are mandatory.


### prism-protos
Another lowest level module, grouping all the protobuf models, and providing the compiled versions of those.

A potential risk is the compatibility from scalapb generated code to the Java libraries, which we will discover.


### prism-identity
This module fits on top of prism-crypto/prism-protos, providing anything related to DIDs, like generation/recovery/validation/etc.

This is the place to hook the long-form unpublished DIDs, but, it doesn't involve any network related calls.


### prism-credentials
This module fits on top of prism-crypto/prism-identity, providing the necessary functionality related to credentials, like issuance/verification/revocation/etc, it's supposed to work without network calls.

When we integrate dynamic credential templates/schema, this is going to be the place for those.


### prism-connector
The plan for this module is to fit on top of prism-crypto/prism-identity, providing any necessary stuff for invoking the node, like handling custom DID authentication schema so that users don't need to worry about it.

Note that network calls aren't expected because it would be simpler to use the auto-generated clients in any language.
