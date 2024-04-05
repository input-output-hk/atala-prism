# Operations Lifecycle

## How PRISM Node processes operations

User issues new operations using one of these gRPC calls:
1. Every operation has its corresponding gRPC call: `CreateDID`, `UpdateDID`, `ProtocolVersionUpdateOperationType`.
2. In addition to this, we have a gRPC call for sending several operations at once: `PublishAsABlock`.

After receiving operations in any of these calls, Node service forwards them to the `objectManagementService.sendAtalaOperations` method.

## How sendAtalaOperations works

`sendAtalaOperations(op: node_models.SignedAtalaOperation*): Future[List[AtalaOperationId]]`

This method does the following:
- Accepts a list of [SignedAtalaOperation](https://github.com/input-output-hk/atala-prism-sdk/blob/master/protosLib/src/main/proto/node_models.proto#L147)
- Creates a new [AtalaObject](https://github.com/input-output-hk/atala-prism-sdk/blob/master/protosLib/src/main/proto/node_internal.proto#L18)
- Serializes the `AtalaObject` into an array of bytes using protobuf
- Stores `(objectId, objectBytes)` into `atala_objects` database table
- Stores every operation into `atala_operations` table with a new status `RECEIVED`
- The method returns a list of operation identifiers. Users should use these identifiers for tracking operation statuses.

**NOTE:** that sendAtalaOperations doesn't immediately publish operations to the ledger. Instead, it just stores those into Node's database for further scheduled publishing.

### Duplicate operations

PRISM Node service should not corrupt databases when a user sends the same operation twice for some reason.

When we insert a new operation in `sendAtalaOperations` method, we have the following line in the corresponding SQL query:
```sql
ON CONFLICT (signed_atala_operation_id) DO NOTHING
```
It means that we do not publish this operation for the second time.

## Publishing operations to the Ledger.

Another service `SubmissionService` publishes operations to the ledger periodically. 
Before publishing operations to the public Cardano network, we merge them into blocks. This is supposed to reduce costs and to improve performance consumptions.
Now we have two scheduled tasks for this: `SubmitReceivedObjects` and `RetryOldPendingTransactions`:
Both of these tasks are supposed to retrieve Atala operations, merge them into larger blocks if this doesn't break the limits, and submit them to the Cardano network.
- `SubmitReceivedObjects` retrieves operations which were received via `publishAtalaOperations`.
- `RetryOldPendingTransactions` retrieves operations that the service did not submit to the Cardano ledger because of an error. For example, it may happen when we don't have enough money to cover fees.

### SubmitReceivedObjects workflow

We schedule this task periodically with a configured period of time `operationSubmissionPeriod`. We also have an additional `FlushOperationsBuffer` gRPC call to start this task immediately.
In this method we do the following:
- Extract all objects that have not been submitted yet. This means that the `processed` flag is `false`, and there's no `transactionId` matched with this object.
- Merge consecutive AtalaObjects into larger ones. We use the mergeIfPossible method that checks if it's possible to merge two objects without breaking the restrictions for transaction metadata size.
- From the previous step we get pairs of `(newAtalaObject, listOfOldAtalaObjects)`. On this step we perform several database updates for every such pair:

  - Insert new objects to `atala_objects`.
  - Mark old objects as `processed`.
  - Update the corresponding `AtalaObjectId` for the operations.
- For every `newAtalaObject` from the previous step, we call `ledger.publish()`, and store the corresponding `transactionId` in `atala_object_tx_submissions`.
- `ledger.publish()` creates a new transaction in the cardano-wallet using REST API. See the [documentation](https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction) for possible errors. Please note that the `cardano-wallet` performs the actual publishing to the Cardano network.


### RetryOldPendingTransactions workflow

This task is scheduled similarly to `SubmitReceivedObjects` with a configured period of time `transactionRetryPeriod`.
In this method we do the following:
- Retrieve transactions with status PENDING in the Node's database.
- PENDING status from the previous step may be outdated, so for every transaction we get the latest status using `getTransactionDetails` REST API call to the `cardano-wallet` service. Note that some of the transactions may still be pending because the public network is unavailable now or because of issues like a lack of money to cover the fees.
- From the previous step we get two lists of transactions: `inLedgerTransactions` and `pendingTransactions`.
- For transactions from `inLedgerTransactions` we just set `InLedger` status in Node's database.
- For transactions that are still pending we call `mergeAndRetryPendingTransactions` method that deletes old transactions, tries to merge objects, and creates new transactions.

#### mergeAndRetryPendingTransactions workflow

- On every transaction we send deleteTransaction REST API request to the `cardano-wallet` service.

  - If `cardano-wallet` returned `TransactionAlreadyInLedger` error, then we update the corresponding status in the Node's database to `InLedger`.
  - If `cardano-wallet` returned any other error (it may be whether "wallet not found" or "not acceptable header"), then we ignore it in order to retry on the next iteration.
  - If `cardano-wallet` successfully deleted the transaction, we set the transaction's corresponding status in the Node's database to `Deleted`.
- Then we retrieve objects from the transactions deleted on the previous step.
- Merge consecutive objects into larger ones when it's possible without exceeding metadata size limits.
- For every object from the previous step, we call `ledger.publish()`, and store the new `transactionId` in `atala_object_tx_submissions`.


## How PRISM Node processes operations approved by the Ledger

- We use `cardano-wallet` for publishing transactions to the Cardano network.
- We use `cardano-db-sync` service to store synchronized with the public Cardano network state.
- On PRISM Node side we have a scheduled task `syncAtalaObjects`.

### syncAtalaObjects workflow

- Retrieve the index of the latest synchronized block from Node's key-value storage to `lastSyncedBlockNo`.
- Retrieve the index of the latest confirmed block from `cardano-db-sync` to `lastConfirmedBlockNo`.
- Process all new transactions in blocks from `lastSyncedBlockNo + 1` to `lastConfirmedBlockNo`.
- In every new block, we iterate over transactions. In every transaction, we check if it has relevant metadata that follows PRISM protocol.
- For every relevant metadata, we retrieve AtalaObjects that are now considered confirmed.
- On every new AtalaObject we call `BlockProcessingService.processBlock()`.
- `BlockProcessingService.processBlock()` iterates over operations in the block, validates the operation, and tries to apply it to its state.
