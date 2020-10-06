# Atala PRISM Identity

This project is an identity tooling library cross-compiled for Scala and ScalaJS, so Atala PRISM can use it in both the
[backend](../credentials-verification) and the [browser wallet extension](../credentials-verification-webextension).

## JavaScript
There is an SDK version that runs on JavaScript, try it with:
- Run: `sbt "project identityJS" "fastOptJS::webpack"`
- Pick the js library from our project: `js/target/scala-2.12/scalajs-bundler/main/identity-fastopt-bundle.js`

## Android
The library is prepared to run on Android, to do so, you need to build a fat-jar manually which includes the Scala stdlib, then, import such jar in the Android project:
- Run `sbt "project identityJVM" "assembly"`
- Then, grab the jar from `jvm/target/scala-2.12/prism-identity.jar`
- On your Android project, invoke `io.iohk.atala.identity.DID.createUnpublishedDID` to verify the integration works.

