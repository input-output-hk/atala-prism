Before **Holder** is able to send its credential to **Verifier**, it needs to accept the **Verifier's** connection.

## Accepting the Verifier's connection

Given that these are the same steps followed by **Holder** in a [previous section](/wiki/integration-tutorial/holder-accept-issuer-connection), the whole code for this section is left without much explanation.

One notable difference is that the second **Holder's DID** is used:

```kotlin
val holderAcceptsVerifierConnectionRequest = AddConnectionFromTokenRequest(token = verifierConnectionToken)
val holderVerifierConnection = runBlocking {
    connector.AddConnectionFromTokenAuth(
        holderAcceptsVerifierConnectionRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID2.value,
            holderMasterKeyPair2.privateKey,
            holderAcceptsVerifierConnectionRequest
        )
    )
        .connection!!
}
println("Holder (DID 2): Connected to Verifier, connectionId = ${holderVerifierConnection.connectionId}")
```
