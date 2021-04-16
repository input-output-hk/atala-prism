An astute reader may have noticed that the credential itself is not stored in **Cardano**. Only a proof of its existence is, which means that there is a need to send the credential to **Holder**.

In previous step,  connection between **Issuer** and **Holder** was established. Such connection allows them to exchange messages, hence the issued credential will be sent to **Holder** by using this connection.

While **Connector** allows to send any kind of messages, this tutorial uses our `AtalaMessage` abstraction to define which messages can be exchanged.

## Prepare the message

Let's prepare a message with the issued credential. Which means the message must include the signed credential, and the proof of its inclusion required to verify its validity:

```kotlin
val credentialFromIssuerMessage = AtalaMessage(
    AtalaMessage.Message.PlainCredential(
        PlainTextCredential(
            encodedCredential = holderSignedCredential.canonicalForm,
            encodedMerkleProof = holderCredentialMerkleProofs.first().encode()
        )
    )
)
```

## Get the connection
Before sending the credential, `connectionId` is required to communicate with **Holder**. There is an API that retrieves the connection given the token generated in a [previous section](issuer-generate-identity):

```kotlin
// Issuer needs the connection id to send a message to Holder, which can be retrieved
// from the token generated before.
val issuerGetConnectionRequest = GetConnectionByTokenRequest(issuerConnectionToken)
val issuerHolderConnectionId = runBlocking {
    connector.GetConnectionByTokenAuth(
        issuerGetConnectionRequest,
        RequestUtils.generateRequestMetadata(issuerUnpublishedDID.value, issuerMasterKeyPair.privateKey, issuerGetConnectionRequest)
    ).connection?.connectionId!!
}
```

## Send the credential

Now, proceed to send the actual message that includes the credential:

```kotlin
// the connector allows any kind of message, this is just a way to send a credential but you can define your own
val issuerSendMessageRequest = SendMessageRequest(
    issuerHolderConnectionId,
    pbandk.ByteArr(credentialFromIssuerMessage.encodeToByteArray())
)
runBlocking {
    connector.SendMessageAuth(
        issuerSendMessageRequest,
        RequestUtils.generateRequestMetadata(
            issuerUnpublishedDID.value,
            issuerMasterKeyPair.privateKey,
            issuerSendMessageRequest
        )
    )
}
println("Issuer: Credential sent to Holder")
```
