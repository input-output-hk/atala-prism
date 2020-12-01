# Atala PRISM SDK

This project is a compilation of cross-compiled Scala/Scala.js SDK modules.

## Usage

A fat JAR of a specific module can be created by either using `assembly` (if you only care about Scala 2.13 artefact) or `+assembly` (if you want both Scala 2.12 and 2.13 artefacts) SBT task.

For example, in order to create a cross-compiled prism-crypto fat JAR, run the following command in project root:
```
$ sbt +prismCryptoJVM/assembly
```

You can find the resulting fat JARs in `crypto/jvm/target/scala-2.12/prism-crypto.jar` and `crypto/jvm/target/scala-2.13/prism-crypto.jar`.

If you want to locally publish modularized version of SDK use:

```
$ sbt +sdkJVM/publishLocal
```

That can also be done for single module (e.g. `sbt +prismConnectorJVM/publishLocal`), but in such case you need to make sure you publish all dependencies as well.

## Docs

## Grpc API
First of all, make sure to generate the docs for our grpc API, which is done by running this command:

```shell script
docker run --rm \
  -v $(pwd)/docs/src/main/paradox:/out \
  -v $(pwd)/protos:/protos \
  pseudomuto/protoc-gen-doc --doc_opt=markdown,grpc-api.md

sed -i "1s/.*/# Grpc API reference/" docs/src/main/paradox/grpc-api.md
```

Then, you can proceed to generate the website.

## Website
The [docs](docs) are type-checked by [mdoc](https://github.com/olafurpg/mdoc), and compiled to a website by [paradox](https://github.com/lightbend/paradox) and [sbt-site](https://github.com/sbt/sbt-site), while updating them.
 
Run:
- `sbt prismDocs/previewSite` to launch the preview at [localhost:4000](https://localhost:4000).
- `sbt prismDocs/makeSite` to build the website at `docs/target/site/`

If for any reason, you get this error, make sure to run `sbt prismDocs/clean` then retry:

```
[error] Unexpected top-level pages (pages that do no have a parent in the Table of Contents).
[error] If this is intentional, update the `paradoxRoots` sbt setting to reflect the new expected roots.
[error] Current ToC roots: [connector.html, crypto.html, identity.html, index.html]
[error] Specified ToC roots: [index.html]
[error] Paradox failed with 1 errors
[error] (prismDocs / Compile / paradoxMarkdownToHtml) com.lightbend.paradox.sbt.ParadoxPlugin$ParadoxException
```

## Troubleshooting
If you get errors while the project gets imported by IntelliJ, try running IntelliJ from the terminal:, when dealing with scalajs, npm is required, sometimes IntelliJ fails to get the proper `PATH`:
- `./bin/idea.sh` from the IntelliJ directory, for Linux.
- `open -a "IntelliJ IDEA"` or `open -a "IntelliJ IDEA CE"` for Mac.


This occurs commonly when dealing with scalajs, because npm is required, and, sometimes IntelliJ fails to get the proper `PATH`, example error log:

```
[error] (ProjectRef(uri("file:/home/dell/iohk/repo/cardano-enterprise/prism-sdk/"), "sdkJS") / ssExtractDependencies) java.io.IOException: Cannot run program "npm" (in directory "/home/dell/iohk/repo/cardano-enterprise/prism-sdk/js/target/scala-2.13/scalajs-bundler/main"): error=2, No such file or directory
```
