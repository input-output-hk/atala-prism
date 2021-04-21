It is time for **Holder** to receive the credential. To do so, **Holder** has to query **Connector** for the incoming messages, parse them into the proper model and extract the credential details from it.

## Receive the message

Let's ask **Connector** for messages addressed to **Holder** and take the first one (which should be the one sent from the **Issuer**):

```kotlin
val holderGetMessagesRequest = GetMessagesPaginatedRequest(limit = 1)
val holderReceivedMessage = runBlocking {
    connector.GetMessagesPaginatedAuth(
        holderGetMessagesRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID.value,
            holderMasterKeyPair.privateKey,
            holderGetMessagesRequest
        )
    ).messages.first()
}
```

## Decode the message

Decode the message, and you will get the signed credential and its inclusion proof:

```kotlin
val holderReceivedCredential = AtalaMessage
    .decodeFromByteArray(holderReceivedMessage.message.array)
    .plainCredential!!
println(
    """
    Holder: Message received
    - Canonical credential = ${holderReceivedCredential.encodedCredential}
    - Inclusion proof = ${holderReceivedCredential.encodedMerkleProof}
    """.trimIndent()
)
```

## Next

Later, we will use this credential to share it with **Verifier**.
