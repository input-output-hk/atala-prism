# PRISM iOS wallet

## gRPC PoC

### Running the server

We use [nvm](https://github.com/nvm-sh/nvm) to handle node versions.

Prior to running the server, install the dependencies with `npm install`.

Lastly, to run the server use `node simple_proto_server.js`.

## Running the app

Prior to building the app it's necessary to generate the Swift Package for the Kotlin Crypto library. For this navigate to the `prism-kotlin-sdk` directory on the Terminal and run th following command:
```
$ ./gradlew :crypto:build :crypto:createSwiftPackage
```
Then navigate to the `prism-ios-wallet` directory and run the following command to install all Cocoapods dependencies:
```
$ pod install
```
Once th installation is completed, open the file `prism-ios-wallet.xcworkspace` with Xcode to run the app. Please for all non production runs make sure the scheme `AtalaPrism-Debug`  is useed to avoid populatig Firebase Analytics with test data.


## Proto config

For now due to compilation issues all protos are added to "connector_api.proto"
Every file is identified by a "// MARK: -" comment at the begining
