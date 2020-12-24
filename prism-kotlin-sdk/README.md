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

Lastly, re-import your iOS project in XCode, and you should be all good to go.