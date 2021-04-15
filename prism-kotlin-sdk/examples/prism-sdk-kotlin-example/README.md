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
