# The life cycle of an AtalaOperation

The goal of this document is to explain the current flow that an AtalaOperation, the issues we are discussing in the node implementation, and the
proposed solutions so far


The flow of an operations looks as follows:
1. A user creates and signs the operations using our SDK
1. The user sends the operation to the node (either through an operation specific RPC like createDID, updateDID, or through publishAsABlock)
1. The node receives the operation and stores it in a db table as received. It returns an operation id to the user for him to track the operation 
   status
1. The node periodically polls the table with received operations and gathers them in blocks of `ATALA_BLOCK_SIZE`
1. The node submits one transaction per block generated in the previous step

## Current issues

### Operations ordering

There are some operations in our protocol that have an explicit ordering required. For instance, `UpadteDID` operations point to a previous update or 
a `CreateDID` using a `previousHash` field. `RevokeCredentials` operations also point to an `IssueBatch` operation. In other cases, there is an 
implicit order between operations. For example, if a user submits an `IssueBatch` operation that is signed with `key1`, and then an `UpdateDID` 
operation that revokes `key1`, then the protocol will only consider the sequence of operations as valid if they are processed in that specific order. 
This is, the user could gather operations in a single transaction in an atala block `B` of the form `[issueBatch, update]`. However, when the 
transaction `tx1` carrying `B` goes to the Cardano mempool, it is theoretically possible for an attacker to see the atala block `B`, extract the 
`update` operation and submit it in a separate transaction `tx2`. If `tx2` is confirmed before `tx1`, then the `issueBatch` operation will be rejected
by the PRISM nodes (because it would be signed with a revoked key).
When could this occur?
- Regular key rotation for security good practice
- An issuer could issue some batches, and suddenly detects a batch issued that he does not recognizes. He would like to submit an `updateDID` event 
  revoking the issuing key while there may still be other valid `issueBatch` events waiting for confirmation

The above scenario could be more problematic in cases where the atala blocks are full of dependent operations. This could happen either because of 
sub-optimal use of batching, or due to high-throughput demand. 

Note that it is not a problem if the user sends a block with operations `[update, issueBatch]` where `issueBatch` is signed by a key added in the 
`update` operation. This is because if the attacker sends the `issueBatch` operation to the chain, it will just be consider invalid by PRISM nodes, 
and later will be considered valid in the block.

We could mitigate this scenarios by:
- Waiting for all events associated to a key that will be revoked to be confirmed by PRISM nodes (this takes many minutes) before submitting key 
  revocation. This could become an issue if multiple nodes send operations associated to the same key, but we see this unlikely.
- requesting the clients an smarter use of keys. If a user knows he will revoke a key, ask him to not send events dependent on that key. E.g. instead
  of sending an operation, and then revoke the key that signs it, suggest to first rotate a key, and then perform operations with a new key.

Some other alternatives are:
- add a multisig like operation (not that easy)
- add a previousHash to ALL our operations except for CreateDID. This would make all ordering explicit, and would make the attack described above 
  impossible because the order is imposed by the previousHash fields and those are tamper proof due to the operation signature. The main issue is that
  this could make multi-node management more complex in the future. Imagine multiple node issuing credentials using different keys that belong to the 
  same DID. 

For developer experience, we incline to believe that adding waiting time for the case of key rotation will be enough. We believe that in the regular 
use patter, it won't be a frequent situation. We could also give a warning to a user when we sends an update that will cause a delay and suggest the
user to delete operations that depend on a revoked keys, and re-sign the operations with another one.

### Wallet overload and general throughput

Today we are calling the Cardano wallet without properly managing errors. We created a story to improve this.
One particular problem is that the Cardano wallet may not have enough available UTxOs to send the needed number of transactions. Note that this is
different from not having enough funds. A Cardano transaction consumes a number of UTxO (inputs), and creates new ones (called outputs). 
- The wallet will start with one UTxO, 
- Imagine that the first transaction will create an output with X ADA (today 1 ADA), and the remaining ADA (minus fees) will be located in another 
  output of the transaction. This will leave us with 2 UTxOs that will be available for the next transaction. However, the two new UTxOs won't be 
  usable (as inputs of new transactions) until the transaction that creates them is confirmed. 

A simple rule of thumb is

   The more UTxOs with enough ADA we have, the more parallel transactions we can send.

Now, if the node queue has enough operations to fill many transactions we have two challenges to solve
1. Minimize the waiting time to get all operations confirmed
1. Guarantee order of operations, which may force to add waiting time (as mentioned by an alternative in previous section)

There are a few important observations to tackle these challenges
1. The time required to submit all transactions has a dependency on wallet UTxO distribution
1. The wallet does not provide a rich API to manage transactions yet. But we can call 
   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWalletUtxoSnapshot and 
   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet to get available balances and a notion of the UTxOs available.


In order to increase the parallel submission throughput, we will have to implement an heuristic to increase the UTxO set size. 
For example, every X minutes, we could call `getWalletUtxoSnapshot` and obtain the maximum amount in a single UTxO, say M. Then if M is above certain 
threshold, we could create a transaction that sends ~2 ADA (this is DEPOSIT + MAX FEE) to a number N of outputs (we need to measure the limit based 
on transaction size). This may force the wallet to break the output with M ADA into small UTxOs that can later be consumed in parallel. 
Unfortunately, we cannot guarantee if this will work, as the wallet uses an internal coin selection algorithm. We should refine this strategy, but it
was a suggestion from the wallet team.

On the second topic, in cases where the queue is big, we may have atala blocks that contain transactions that depend one upon the other. For example,
an `issueBatch` operation in one block may depend on a DID update located in the other block.
We can classify operations as follow:
1. build a dependency graph between operations. This is, for a queue that contains the ordered operations `o1, o2,..., on` we say that `oi` depends 
   on `oj` iff
   - j < i and any of the following is valid
     + `oi.previousHash == hash(oj)`, or  <-- these can be sent in the same block safely
     + 'oj` adds the key that signs `oi`, or   <-- these can be sent in the same block safely because is a key addition
     + 'oj' is signed by a key that is revoked by `oi`  <-- here is the only case where `oi` needs to wait for `oj` to be confirmed

   NOTE: In the third case, if the user does not want to wait, he could have first send the update revoking the key, and adding a new key, and then
         add `oj` signed with the newly added key. We can even notify this to the user.

   The graph should be composed by a set of directed graphs
   All nodes in a sub-graph should be associated to the same DID. We can recognized some use errors while constructing the graph, e.g.
     if we have a directed loop, this indicates an error on user side
     if two updateDIDs point to the same previousHash, we have a conflict to inform to the user
     if an operation has a previousHash field that matches no know operation
     if an operation is signed with an unknown key
   Different sub-graphs will be completely independent and would be posted in any order. These could share an atala block and allow for the use of 
   parallel transactions. We also note that the only vertexes that will generate connections to many other vertexes are:
     + `UpdateDID`s as they can add/remove keys used in other operations
     + `IssueBatch`s which can be the `previousHash` of multiple `RevoceCredentials`
   Ideally, a sub-graph will fit in a single transaction and there will not exist dependencies of type 3 (a key is revoked by an update, but we need
   to guarantee order between that update and an operation that uses the said key). If we estimate an average operation size of 500 bytes, then we 
   should be able to allocate ~30 operations in a single atala block
   There are then two cases that become troublesome.
   1. when a sub-graph without type dependencies does not fit in a single transaction.
      Let B1,..., Bn the dependent Atala blocks that contain a sub-graph that does not fit in a single block.
      Let Txi the transaction that carries block Bi.
      We want Txj to be confirmed before Tx{j+1} for each j \in {1,..., n-1}
      Given the limited control on transaction construction, we could send Txi and wait until fully confirmed before sending Tx{i+1}. If we do not 
      wait, we may have issues related to rollbacks.
      While we wait, we can still send transactions involved with other atala blocks
   1. When the sub-graph does fit in a single transaction, but we have dependencies of type 3. In this case, we can split the sub-graph into two 
      blocks, treat them as dependent blocks that do not fit in a single transaction, and proceed as described before. 

## Summary

The problem that arises from submitting a key revocation while we have pending operations to be published that depend on the revoked key, looks 
manageable by suggesting the user to use keys in a smarter way. If we later develop a wallet backend logic, we would be able to manage this on
behalf of the user, as the wallet could encapsulate the logic of querying the node for dependencies and use keys in a reasonable way.
Other submission delays caused by dependencies would only occur if there are so many dependent operations that they would not fit in a single block.
This represents according to our tests to a number above 30 dependent operations. In practice, this looks reasonable too. Remember that the only 
operations that could create dependencies are DID updates and credential revocations.

