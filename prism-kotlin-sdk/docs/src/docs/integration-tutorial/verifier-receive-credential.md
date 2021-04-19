Now it's time to for **Verifier** to receive the credential from **Holder**, this is very similar to a [previous step](/wiki/integration-tutorial/holder-receive-credential), which is why you will see the necessary code without much explanation:

```kotlin
val verifierGetMessagesRequest = GetMessagesPaginatedRequest(limit = 1)
val verifierReceivedMessage = runBlocking {
    connector.GetMessagesPaginatedAuth(
        verifierGetMessagesRequest,
        RequestUtils.generateRequestMetadata(
            verifierUnpublishedDID.value,
            verifierMasterKeyPair.privateKey,
            verifierGetMessagesRequest
        )
    ).messages.first()
}
val verifierReceivedCredential = AtalaMessage
    .decodeFromByteArray(verifierReceivedMessage.message.array)
    .plainCredential!!
println(
    """
    Verifier: Message received
    - Canonical credential = ${verifierReceivedCredential.encodedCredential}
    - Inclusion proof = ${verifierReceivedCredential.encodedMerkleProof}
    """.trimIndent()
)
```