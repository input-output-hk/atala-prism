# Atala PRISM SDK - Identity

To install the module:
```scala
libraryDependencies += "io.iohk" %% "prism-identity" % "@VERSION@"
```

## Decentralized Identifiers

Atala PRISM Identity module provides the necessary tools to work with decentralized identifiers (DIDs).

All DIDs start with URI scheme identifier `did`. Atala PRISM DIDs are no exception, but they also have a specific [DID method](https://www.w3.org/TR/did-core/#dfn-did-methods) attached to them:
```scala mdoc
import io.iohk.atala.prism.identity.DID

DID.prismPrefix
```

Furthermore, there are two forms of method-specific identifiers following after the PRISM prefix: canonical and long. You can build both forms by using these utility methods:
```scala mdoc
val stateHash = "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
val encodedState = "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

val canonical = DID.buildPrismDID(stateHash)
val long = DID.buildPrismDID(stateHash, encodedState)
```

You can also check if a given DID is of a specific form or even parse their form:
```scala mdoc
canonical.isCanonicalForm
long.isLongForm

canonical.getFormat
long.getFormat
```

Finally, it is possible to create a simple DID consisting of a single master key:
```scala mdoc
import io.iohk.atala.prism.crypto.EC

// Set up your public key
val masterKey = EC.generateKeyPair().publicKey

DID.createUnpublishedDID(masterKey)
```
