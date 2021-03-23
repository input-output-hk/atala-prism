# Connector

Use this code to install the Connector module:
```scala
libraryDependencies += "io.iohk" %% "prism-connector" % "@VERSION@"
```

## Requests

This module enables the signature of Connector requests, but first you need to instantiate `RequestAuthenticator` with `EC`:
```scala mdoc
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC

val authenticator = new RequestAuthenticator(EC)
```

You can now sign an arbitrary request:
```scala mdoc:to-string
// Set up your private key
val privateKey = EC.generateKeyPair().privateKey

val request = authenticator.signConnectorRequest(Array(0.toByte), privateKey)
```

`RequestAuthenticator` signs the provided data *and* appends a random nonce to it, which you can examine with this code:
```scala mdoc:to-string
request.requestNonce

// You can also get the Base64 encoded variant
request.encodedRequestNonce
```
