# Atala PRISM Crypto

This project is a crypto library cross-compiled for Scala and ScalaJS, so Atala PRISM can use it in both the
[backend](../credentials-verification) and the [browser wallet extension](../credentials-verification-webextension).

## JavaScript
There is an SDK version that runs on JavaScript (intended to run on iOS for now), try it with:
- Run: `sbt "project cryptoJS" "fastOptJS::webpack"`
- Pick the js library from our project: `js/target/scala-2.12/scalajs-bundler/main/crypto-fastopt-bundle.js`
- Or, try it on your browser by loading the [index.html](index.html) on your browser, or run a local file server like `http-server -a localhost -p 1234 ./` and go to `localhost:1234/index.html`
- Then, in the developer console you can invoke `PrismSdk.hello("you")` or `PrismSdk.generateKeyPair()`

## Android
The library is prepared to run on Android, to do so, you need to build a fat-jar manually which includes the Scala stdlib, then, import such jar in the Android project:
- Run `sbt "project cryptoJVM" "assembly"`
- Then, grab the jar from `jvm/target/scala-2.12/prism-crypto-{version}.jar`
- On your Android project, invoke `io.iohk.atala.crypto.AndroidEC.generateKeyPair()` to verify the integration works.
