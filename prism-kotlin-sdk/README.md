# Atala PRISM Multiplatform SDK

This is a proof of concept PRISM SDK that is cross-compiled to JVM/JS/iOS-ARM64.

## How to build the SDK

Make sure that you have cocoapods and cocoapods-generate installed:
```
$ sudo gem install cocoapods
$ sudo gem install cocoapods-generate
```

You should be able to import and build the project in IntelliJ IDEA now. If you experience issues with missing cocoapods modules try installing the pods manually:

```
$ ./gradlew podspec
...
$ pod gen
...
$ cd gen/prism_kotlin_sdk && pod install
...
```

## How to use for Android app

In order to use the SDK for the Android app, first you have to build an artifact and publish it to the local Maven repository:
```
$ ./gradlew publish
```

This generates JAR artifacts under `~/.m2/repository/io/iohk/atala/prism` which you can use in your Android app by including this in your gradle build file:
```
dependencies {
    implementation "io.iohk.atala.prism:prism-kotlin-sdk:0.1-SNAPSHOT"
}
```

## How to use for iOS app

This project can be included as a Cocoapods dependency. First you need to generate a Podspec file that will be included by XCode:
```
$ ./gradlew podspec
```

Navigate to your iOS application's folder and edit your Podfile so that it depends on the SDK:
```
target 'ios-app' do
    pod 'prism_kotlin_sdk', :path => '../prism-kotlin-sdk'
end
```

Now install the dependencies:
```
$ pod install
```

Re-import your iOS project in XCode, and you should be all good to go.

At last, make sure to run some Kotlin code in the Main thread to avoid memory leaks (until [KT-44132](https://youtrack.jetbrains.com/issue/KT-44132) is resolved), for example:

```swift
// Add this as the last line of this method:
// application(_ application: UIApplication, didFinishLaunchingWithOptions  launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
SHA256Digest.Companion().compute(bytes: [0, 1, 2])
```

## Docs

Be aware that the [integration-tutorial](docs/src/docs/integration-tutorial) doesn't use Ank for compile-checked docs because we are blocked by this [issue](https://github.com/arrow-kt/arrow/issues/472). The problem is that the circleci job that builds the website docs doesn't have the connector/node running which are necessary to perform some network calls from the docs.


### Grpc API
First, make sure to generate the docs for our grpc API, which is done by running this command:

```shell script
docker run --rm \
  -v $(pwd)/docs/src/docs/grpc:/out \
  -v $(pwd)/../prism-sdk/protos/src:/protos \
  pseudomuto/protoc-gen-doc --doc_opt=/protos/resources/markdown.tmpl,grpc-api.md
```

### Add logos
To improve UX/UI of the docs logos and icons can be added to docs. All these files are already a part of the [Interactive Demo section](../prism-interactive-demo-web/public). To add them to these docs execute: 

```shell
mkdir -p docs/src/orchid/resources/assets/media

cp ../prism-interactive-demo-web/public/favicon.ico docs/src/orchid/resources
cp ../prism-interactive-demo-web/public/images/atala-prism-logo-suite.svg docs/src/orchid/resources/assets/media
```

Then, you can proceed to generate the website.

### Website

The [docs](docs) are type-checked by [arrow-ank](https://github.com/arrow-kt/arrow-ank), and compiled to a website by [orchid](https://orchid.run/), while updating them.

Run:
- `./gradlew :docs:orchidServe` to launch the preview at [localhost:8080](https://localhost:8080).
- `./gradlew :docs:orchidBuild` to build the website at `docs/build/docs/orchid`

