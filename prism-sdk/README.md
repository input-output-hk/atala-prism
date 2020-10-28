# Atala PRISM SDK

This project is a compilation of cross-compiled Scala/Scala.js SDK modules.

## Usage

A fat JAR of a specific module can be created by either using `assembly` (if you only care about Scala 2.13 artefact) or `+assembly` (if you want both Scala 2.12 and 2.13 artefacts) SBT task.

For example, in order to create a cross-compiled prism-crypto fat JAR, run the following command in project root:
```
$ sbt +prismCryptoJVM/assembly
```

You can find the resulting fat JARs in `crypto/jvm/target/scala-2.12/prism-crypto.jar` and `crypto/jvm/target/scala-2.13/prism-crypto.jar`.
