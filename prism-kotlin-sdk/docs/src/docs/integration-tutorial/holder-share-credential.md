It is time to share the credential with **Verifier**, this is very similar to a [previous step](/wiki/integration-tutorial/issuer-share-credential), which is why the necessary code doesn't need much explanation:

```kotlin
val credentialFromHolderMessage = AtalaMessage(
    AtalaMessage.Message.PlainCredential(
        PlainTextCredential(
            encodedCredential = holderReceivedCredential.encodedCredential,
            encodedMerkleProof = holderReceivedCredential.encodedMerkleProof
        )
    )
)

val holderSendMessageRequest = SendMessageRequest(
    holderVerifierConnection.connectionId,
    pbandk.ByteArr(credentialFromHolderMessage.encodeToByteArray())
)
runBlocking {
    connector.SendMessageAuth(
        holderSendMessageRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID2.value,
            holderMasterKeyPair2.privateKey,
            holderSendMessageRequest
        )
    )
}
println("Holder (DID 2): Credential sent to Verifier")
```