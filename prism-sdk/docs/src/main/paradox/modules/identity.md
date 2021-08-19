# Identity

Use this code to install the Identity module:
```scala
libraryDependencies += "io.iohk" %% "prism-identity" % "@VERSION@"
```

## Decentralized Identifiers (DIDs)

This module provides the necessary tools to work with DIDs.

All DIDs start with the URI scheme identifier `did`, and PRISM's DIDs are no exception. But they also have a specific [DID method](https://www.w3.org/TR/did-core/#dfn-did-methods) attached to them:

```scala mdoc
import io.iohk.atala.prism.kotlin.identity.DID

DID.prismPrefix
```

There can be two forms of method-specific identifiers after the PRISM prefix: Canonical and long. You can build both by using these utility methods:
```scala mdoc
val stateHash = "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
val encodedState = "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

val canonical = DID.buildPrismDID(stateHash)
val long = DID.buildPrismDID(stateHash, encodedState)
```

You can also check a DID's specific form or even parse their form with this code:
```scala mdoc
canonical.isCanonicalForm
long.isLongForm

canonical.getFormat
long.getFormat
```

You can create a simple DID consisting of a single master key with this code:
```scala mdoc
import io.iohk.atala.prism.crypto.EC

// Set up your public key
val masterKey = EC.generateKeyPair().publicKey

DID.createUnpublishedDID(masterKey)
```
