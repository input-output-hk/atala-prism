# Atala PRISM SDK - Connector

To install the module:
```scala
libraryDependencies += "io.iohk" % "prism-connector" % "@VERSION@"
```

## Requests

Atala PRISM Connector module provides the means to sign Connector requests, but first you need to instantiate `RequestAuthenticator` with `EC`:
```scala mdoc
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC

val authenticator = new RequestAuthenticator(EC)
```

Now, you can sign an arbitrary request as follows:
```scala mdoc:to-string
// Set up your private key
val privateKey = EC.generateKeyPair().privateKey

val request = authenticator.signConnectorRequest(Array(0.toByte), privateKey)
```

`RequestAuthenticator` does not simply sign the provided data, but also appends a random nonce to it which you can examine like this:
```scala mdoc:to-string
request.requestNonce

// You can also get the Base64 encoded variant
request.encodedRequestNonce
```
