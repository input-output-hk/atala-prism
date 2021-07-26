# PRISM iOS wallet

## gRPC PoC

### Running the server

We use [nvm](https://github.com/nvm-sh/nvm) to handle node versions.

Prior to running the server, install the dependencies with `npm install`.

Lastly, to run the server use `node simple_proto_server.js`.

## Prepare Environment

The first step to prepare your development environment is to run the '''bash''' script:
```
./bootstrap.sh
```

And that's it!

This script will prepare your environment this includes:
- Install development dependency [Mint](https://github.com/yonaskolb/Mint)
- Build Kotlin Crypto SDK and create Swift Package
- Mint will install the dependencies:
    - [XcodeGen](https://github.com/yonaskolb/XcodeGen)
    - [SwiftLint](https://github.com/realm/SwiftLint)
    - [SwiftFormat](https://github.com/nicklockwood/SwiftFormat)
- Generate protoc components/classes
- Generate Xcode Project
- Install Cocoapods

## (Skip if you run Bootstrap) Build Crypto SDK and create Swift Packages

```
cd ../prism-kotlin-sdk
$ ./gradlew :crypto:build :crypto:createSwiftPackage
```

## (Skip if you run Bootstrap) Generate XCode project

This will prepare the project aswell as generating protoc classes and install cocoapods

```
$ mint run xcodegen generate
```


## Running the app

Once the installation is completed, open the file `prism-ios-wallet.xcworkspace` with Xcode to run the app. Please for all non production runs make sure the scheme `AtalaPrism-Debug`  is useed to avoid populatig Firebase Analytics with test data.

