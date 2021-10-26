# Off-Chain Atala Operations validator

According to the operation lifecycle process described [here](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/node/operaions-lifecycle-in-node-service.md), PRISM Node applies operations only after publishing them to the Ledger. 
However, PRISM Node could validate some information in advance before interacting with the Ledger. Therefore:
- it should help the Node respond to users faster and give a more detailed explanation of the errors.
- in addition to the better user experience, it also gives us invariant that there are no cycles in the operations dependency graph.

## Off-chain validation errors

Here's the list of errors that could be caught during the off-chain validation

- `PreviousOperationDuplication`: all operations should have a unique previous operation hash.
- `KeyAlreadyRevoked`: a key used for signing a new operation should not be revoked before.
- `KeyUsedBeforeAddition`: a key used for signing a new operation should be present in the corresponding _DID Document_.
- `DidUsedBeforeCreation`: `DID` should be created before its usage.

## Operation interface enhancement

The proposal is to add two new methods to every Operation interface:

- The first method makes an off-chain validation of the operation:
```kotlin
def verifyOffChain(signedWithKeyId: String)(implicit
       operationsVerificationRepository: OperationsVerificationRepository[IO],
       credentialBatchesRepository: CredentialBatchesRepository[IO]
   ): EitherT[IO, errors.NodeError, Unit]
```

- The second method partially applies operation off-chain: it doesn't update credentials, but it involves the corresponding key additions and removals to validate further operations dependent on this one:
```kotlin
override def applyOffChain(signedWithKeyId: String)(implicit
       operationsVerificationRepository: OperationsVerificationRepository[IO],
       credentialBatchesRepository: CredentialBatchesRepository[IO]
   ): EitherT[IO, errors.NodeError, Unit] 
```

## verifyOffChain & applyOffChain call

When Node receives a new operation from a user, `ObjectManagementService.scheduleAtalaOperations` is invoked.
To schedule a new operation, we call `atalaOperationsRepository.insertOperation`. The proposal is to wrap this method with validation calls the following way:

```kotlin
for {
    _ <- offChainVerifierService.verifyOffChain(signedAtalaOperation)
    countInserted <- atalaOperationsRepository.insertOperation(...)
    _ <- offChainVerifierService.applyOffChain(signedAtalaOperation)
}
```

So that `offChainVerifierService` represents the current state of the public and private keys following scheduled operations.

## How to maintain the state for validation

To validate operations, we need to store additional data:

- We have to maintain the state of _DID documents_
- We have to store the set of previous operation hashes

We could add new tables in the postgres to store this information or use in-memory representation.