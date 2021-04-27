# PRISM SDK Kotlin Example
This project has the Kotlin usage examples for the PRISM SDK, the goal is to allow you to do a PRISM integration by following these examples.

The PRISM SDK supports Android, you should be able to use these examples by switching to the android artifacts (`protos-android`, `crypto-android`, etc).


## Usage

For now, PRISM SDK needs to be built manually by running `./gradlew publishJvmPublicationToMavenLocal` in the PRISM SDK root folder. Then, you are expected to pick the library version (it will look like `0.1.0-$gitHash`), and assign it to the `prismVersion` variable in [build.gradle.kts](./build.gradle.kts).

You are expected to have the connector backend available in `localhost:50051`, and the node backed in `localhost:50053`, if that's not the case, replace relevant constants in the examples with the correct values.

Use the command line to try the example:
- Format the code (required only if you do any changes to it):`./gradlew ktlintFormat`
- Compile: `./gradlew build`
- Run: `./gradlew run`

An alternative is to open [Main.kt](src/main/kotlin/io/iohk/atala/prism/example/Main.kt) with IntelliJ and run it there.

### Cardano Testnet

While the examples can run with the Cardano Testnet, there are some details to be aware of:
- Some examples expect the transactions to be confirmed instantaneously, sleeping after publishing operations could help.
- You will likely benefit from syncing only a few Cardano blocks, which can be done by picking the latest block number from the Testnet, and set it in a variable before running the node: `export NODE_CARDANO_PRISM_GENESIS_BLOCK=1000000` (replace the `1000000` with the latest block).
- In order to get transactions confirmed fast, set this variable before running the node: `export NODE_CARDANO_CONFIRMATION_BLOCKS=1`
