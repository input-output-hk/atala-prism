# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [1.0.0] - Unreleased

[1.0.0]: LINK TO THE GITHUB TAG

### Added
- **Maven and NPM artifacts are now available on Github Packages.**
- A new proto class `AtalaErrorMessage` that represents a gRPC error. `AtalaMessage` can now contain `AtalaErrorMessage` as an underlying message.
- Added a new snippet to Kotlin examples that was used for Africa Special video walkthrough.
- A new method `EC.isSecp256k1` can be used to validate whether the point lies on the curve.
- A new constant `DID.getMASTER_KEY_ID` is introduced.
- A new convenience API `CreateContacts` for creating contacts in bulk from CSV has been introduced.
- New ways to create DIDs from mnemonic phrase: `DID.deriveKeyFromFullPath` and `DID.createDIDFromMnemonic`.
- [JS] Expose all the remaining pieces of SDK to JavaScript (including gRPC remoting).

### Changed
- **[BREAKING CHANGE]** All proto files have been converted to snake_case. This might affect you if you have been using the proto files directly (e.g., if you were auto-generating proto classes that are specific to your programming language). If you are using the supplied proto classes, nothing should change from your perspective.
- **[BREAKING CHANGE]** `DID.stripPrismPrefix` was deleted as it was duplicating `DID.suffix` functionality.
- **[BREAKING CHANGE]** `Credential.isSigned` and `Credentials.isUnverifiable` were replaced with `Credential.isVerifiable` that better suits most use cases.
- **[BREAKING CHANGE]** `RegisterDIDRequest.createDidOperation` was replaced with `RegisterDIDRequest.registerWith`. You can still access the old behaviour by wrapping `SignedAtalaOperation` into `RegisterDIDRequest.RegisterWith.CreateDidOperation`.
- **[BREAKING CHANGE]** All occurrences of `List<Byte>` have been replaced with `ByteArray`.
- **[BREAKING CHANGE]** [JS] JavaScript API has seen a major overhaul, it is much closer to the JVM one now. Most classes and methods have lost `JS` suffix at the end of their names and now accept proper classes instead of hex strings.
- Integration tests module has been deleted. It was not being exposed to users, so nothing should change from user's perspective.
- Kotlin version has been upgraded to 1.5.10. Users that are using 1.4.* should still be able to use the SDK without issues.
- Documentation template for protobuf API has been improved.
- README has been improved a lot:
    - Added a section on how to install all the prerequisites on Linux and macOS
    - Added a section on how to use it from JavaScript

### Fixed
- **[BREAKING CHANGE]** Due to the closure of Bintray, we have moved all custom artifacts to JFrog. Practically, this means users need to replace `https://dl.bintray.com/itegulov/maven` repository with `https://vlad107.jfrog.io/artifactory/default-maven-virtual/`.
- Proper `equals` and `hashCode` for `DerivationAxis`.

## [0.1.0] - 2021-04-27
_Presumably already filled in_
