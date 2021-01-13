Proof of Concept for a ScalaJS browser extension wallet that can be used to sign credentials and authenticate communication with the backend. Currently it is compatible with Chrome only, in the future support of Firefox and new Edge versions is going to be added.

Browser extension model imposes rescrictions on what can be done - e.g. the code running in the tab cannot invoke directly methods of the extension popup. Because of that the wallet consists of three parts:
* background - it is the core of the wallet, manages the keys and signing requests; that is the place where the cryptography is run,
* injected script - code that is injected into sites that need the wallet; in this PoC it just sends a signing request to the background; in the future it is going to expose API that JavaScript code running in tabs will access in order to do the signing,
* popup - ephemeral web page shown in a popup that opens when extension method is clicked; when it is closed, all the state is lost; used to notify user about signing requests and let them choose which ones to sign.

## Dependencies

This project uses [scalajs-bundler](https://github.com/scalacenter/scalajs-bundler) to fetch JavaScript dependencies, so `npm` is required to build it.

Until this [PR](https://github.com/lucidd/scala-js-chrome/pull/46) gets merged, we need a custom plugin to support [content_scripts](https://developer.chrome.com/extensions/content_scripts), if you don't need those, feel free to replace the forked plugin on the [plugins.sbt](project/plugins.sbt).

Before being able to build the extension, you'll need to install a plugin locally.
- Clone `https://github.com/AlexITC/scala-js-chrome`
- Move to the `add-content-script` branch (`git checkout add-content-script`).
- Install the custom `scala-js-chrome` locally (`sbt publishLocal`).

Then, running `sbt chromePackage` on this project is enough.

## Development
- Running `sbt "~chromeUnpackedFast"` will build the app each time it detects changes on the code, it also disables js optimizations which result in faster builds (placing the build at `target/chrome/unpacked-fast`).
  - You can run the following to spin up all required dependencies, except PostgreSQL, along `sbt ~chromeUnpackedFast`:
    ```shell script
    ./run_local.sh
    ```
    That will open a `tmux` session you can kill typing `CTRL+B` followed by `:kill-session`, or just exit each sub-terminal normally.
    
    **NOTE**: You have to have the PostgreSQL DB running already.
    **WARNING**: Beware that killing the session may leave `envoy` running and, next time you try to run it, it will fail to run again, but it's a harmless problem because it would be running anyway.
- Be sure to integrate scalafmt on your IntelliJ to format the source code on save (see https://scalameta.org/scalafmt/docs/installation.html#intellij).

### Run tests
You are required to have Chrome installed in order to run the tests, then, run:
- `CHROME_VERSION="$(google-chrome --version)" sbt test`

Unfortunately, we haven't found a workaround to run the tests from IntelliJ.

### Running

See our [README](../README.md#How-to-run) for instructions.

## Release
Run: `PROD=true sbt chromePackage` which generates:
- A zip file that can be uploaded to the chrome store: `target/chrome/chrome-scalajs-template.zip`
- A folder with all resources which can be packaged for firefox: `target/chrome/unpacked-opt/`

To Run against develop environment: `CONNECTOR_URL=https://grpc-develop.atalaprism.io:4433 sbt chromePackage` which generates:
- A zip file that can be uploaded to the chrome store: `target/chrome/chrome-scalajs-template.zip`
- A folder with all resources which can be packaged for chrome: `target/chrome/unpacked-opt/`
- This will allow you run browser wallet against develop environment. You don't need to run other dependent services locally
- For developer testing upload this folder into the `chrome://extensions/` load unpacked  `target/chrome/unpacked-opt/` 
## Docs
The project has 3 components, and each of them acts as a different application, there are interfaces for interacting between them.

### Active Tab
The [activetab](src/main/scala/io/iohk/atala/cvp/webextension/activetab) package has the script that is executed when the user visits a web page that the app is allowed to interact with, everything running on the active tab has limited permissions, see the [official docs](https://developer.chrome.com/extensions/activeTab) for more details, be sure to read about the [content scripts](https://developer.chrome.com/extensions/content_scripts) too. As the message to be signed will come from the tab, we need to deliver it to the background.

### Popup
The [activetab](src/main/scala/io/iohk/atala/cvp/webextension/popup) package has the script that is executed when the user visits clicks on the app icon which is displayed on the browser navigation bar.
There is a limited functionality that the popup can do, see the [official docs](https://developer.chrome.com/extensions/browserAction) for more details.
It cannot talk directly to the scripts in tab, just the background script.

### Background
The [background](src/main/scala/io/iohk/atala/cvp/webextension/background) package has the script that is executed when the browser starts, it keeps running to do anything the extension is supposed to do on the background, for example, interacts with the storage, web services, and alarms.
As other components can't interact directly with the storage or web services, they use a high-level API ([BackgroundAPI](src/main/scala/io/iohk/atala/cvp/background/BackgroundAPI.scala)) to request the background to do that.

It can be viewed as the central component, as it is the only one that the other components can communicate with.

Be sure to review the [official docs](https://developer.chrome.com/extensions/background_pages).

## Hints
While scalajs works great, there are some limitations, in particular, you must pay lots of attention while dealing with boundaries.

A boundary is whatever interacts directly with JavaScript, like the browser APIs, for example:
- When the background receives requests from other components, it gets a message that is serialized and deserialized by the browser, in order to support strongly typed models, the app encodes these models to a JSON string, the browser knows how to deal with strings but it doesn't know what to do with Scala objects.
- When the storage needs to persist data, the app encodes the Scala objects to a JSON string, which is what the browser understands.
- When there is a need to interact with JavaScript APIs (like external libraries), you'll need to write a [facade](src/main/scala/io/iohk/atala/cvp/webextension/facades) unless there is one available, this facade will deal with primitive types only (strings, ints, etc).

## Troubleshooting
If you get errors while the project gets imported by IntelliJ, try running IntelliJ from the terminal:, when dealing with scalajs, npm is required, sometimes IntelliJ fails to get the proper `PATH`:
- `./bin/idea.sh` from the IntelliJ directory, for Linux.
- `open -a "IntelliJ IDEA"` or `open -a "IntelliJ IDEA CE"` for Mac.


This occurs commonly when dealing with scalajs, because npm is required, and, sometimes IntelliJ fails to get the proper `PATH`, example error log:

```
[error] (ProjectRef(uri("file:/home/dell/iohk/repo/cardano-enterprise/prism-sdk/"), "sdkJS") / ssExtractDependencies) java.io.IOException: Cannot run program "npm" (in directory "/home/dell/iohk/repo/cardano-enterprise/prism-sdk/js/target/scala-2.13/scalajs-bundler/main"): error=2, No such file or directory
```

