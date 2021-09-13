## Subscriptions and their use cases
Before jumping to description of details of subscription mechanism, we will describe some sensible
use cases which might be useful for various parties of the system.

Use cases:
1. Wallet wants to be notified about all operations, which have been confirmed (approved or rejected) in the Cardano network, 
    relevant to DIDs owned by the wallet user. It's useful in order to keep Wallet's state up to date.  
There are a lot of use cases which fall into this category, some of them:  
   * Issuer wants to know about all credential issuance/revocations signed with their issuing key,
     which could have been shared with some trusted party (e.g. departments at the university).
   * Issuer wants to know about a key creation/revocation which happened from another Management Console.
   * A system with two or more Atala nodes, whose API can be used at the same time.
   * Wallet wants to keep track of new user's DIDs.

    _CreateDID_, _UpdateDID_, _IssueCredentialBatch_, _RevokeCredentials_ are operations which a wallet wants to be aware of.

2. Verifier wants to be notified if a _particular_ credential is revoked, and it was approved in the Cardano network.
   Verifier is interested if a credential which was provided by a holder is still valid.

3. Wallet wants to be notified about _particular_ `AtalaOperation`'s status
   (_PENDING_, _CANCELLED_, _REJECTED_, _IN_BLOCK_, _CONFIRMATION_LEVEL n_, _APPROVED_),
   in order to show a user status of operation in real time to make a wallet interface more responsive.

4. Wallet wants to be notified when a node went offline mode and get back to online.

The last two cases and similar ones will be out of the consideration of this document for now,
we will mostly focus on notifications about Atala operations.

## High level overview of the protocol
Suggested protocol is inspired by [BIP-157](https://en.bitcoin.it/wiki/BIP_0157) and [BIP-158](https://en.bitcoin.it/wiki/BIP_0158).

The core structure of this approach is Golomb Coded Set filter (GCS), which is akin a Bloom filter
with smaller serialisation size but slower query time.  
This structure is basically a compressed sorted list of hashes computed for original entries.  
In such list we can check if an entry hash exists, however as we rely on hashes but not on the original entries,
such existence test is probabilistic (despite the probability is close to 1),
consequently, a test can give a false positive result.
Hence, every positive check has to be double-checked on the original list.

The protocol in nutshell: when a node receives an Atala block,
it computes GCS for relevant data of the block operations, sends the resulting GCS to a client,
the client tests if there is at least one event it listens to, and if so the client requests them.  
We offload the testing logic to clients in order to reduce load on a node.

Let's move on to more detailed description.  
Let's assume that a node has the list of subscribers, stored in memory,
and each of those has associated gRPC stream.
Then that will happen on new event:
1. when a block arrives, a node computes a GCS for it, and saves it and GCS in a persistent storage, 
   sends the GCS to all subscribers' streams
2. when a client receives a GCS, it checks if there are entries in the GCS which a client is interested in.  
   If there are any, in a separate connection a client requests an original block which the GCS was derived from, 
   otherwise it saves a corresponding Atala object id as the last known to a local persistent storage.
3. in case, if a client requests a block for GCS, a node responds to the client with it.
   This message is sent to the separate connection, not to the associated stream.
4. upon receiving a block, a client handles it and saves a corresponding Atala object id as the last known.

Pay attention, that instead of building GCS for an Atala block, node could build it for 
any kind of thing, for example, for sequence of block, for an Atala operation, or even
for a node lifetime event (e.g. a node disconnected from the Cardano network).  
This flexibility might be used in future versions of the protocol.

Another subject for discussion is whether we need to store all kind of events persistently. 
For example, a node disconnect event seems unimportant after a node connects back, 
but let's leave this question for future discussion.

Also, in the step 4th it depends on actual implementation who will respond to a client with events.
In the simplest case, it might be a node itself, however, 
in order to reduce load on a node, a reverse proxy or some kind of cache might be in front of the node.

Let's get back to our assumption about the subscribers list on the node, 
and describe how a client will actually connect to a node:
1. a client initiates a stream connection to a node sending a last known Atala object id if any
2. a node responds with GCSs from its persistent storage to the stream
3. a client filters out received GCSs, and requests corresponding events from the node in a separate connection(s)
4. a node responds with requested events
5. a client receives them, update its last known Atala object id, then check that they actually match its filters, and handle matched ones
6. after that, a client moves to the previously described flow

## Filters and related types
In this section we will outline _filter_ types, 
which a client leverages to specify which operations it's interested in.

We start with some auxiliary and abstract definitions.
```scala
sealed trait ConfirmedStatus extends EnumEntry with Snakecase
object ConfirmedStatus extends Enum[ConfirmedStatus] {
  val values = findValues

  final case object AppliedConfirmedStatus extends ConfirmedStatus
  final case object RejectedConfirmedStatus extends ConfirmedStatus
}

abstract class SubscriptionFilter(val status: Option[ConfirmedStatus]) {
  // Hash for GCS filter
  def sipHash: Long
}

// GCS doesn't support insertion, it can be built only for fixed number of elements
class GCS(val operations: List[Operation], val p: Int) {
  val m: Long = 1L<<p
  // ... here code to build a GCS from the passed list on creation ...
  def exists(g: SubscriptionFilter): Boolean = {
    // ... here code to check GCS existence ...
  }
}
```

`status` determines which operation outcomes a filter matches.  
If `None` then any outcome, otherwise only specified one.  
We assume that `GCS` is already defined class.

```scala
sealed trait OperationTag
case object UpdateDidTag extends OperationTag
case object IssueCredentialBatchTag extends OperationTag
case object RevokeCredentialsTag extends OperationTag
case object CreateDidTag extends OperationTag

case class DidOperationFilter(operationTag: OperationTag, 
                              didSuffix: DIDSuffix, 
                              override val status: Option[ConfirmedStatus]) extends SubscriptionFilter(status) {

  override def sipHash: Long = {
    // Hashes tuple (operationTag, didSuffix)
  }
}

case class RevokeCredentialFilter(credentialHash: Sha256Digest,
                                  override val status: Option[ConfirmedStatus]) extends SubscriptionFilter(status) {
  override def sipHash: Long = {
    // Hashes a tuple ("RevokeCredentialFilter", credentialHash)
  }
}
```
These filters correspond to all cases from the first chapter.

## Implementation details
In this section we describe how suggest protocol will be integrated in the node codebase,
and outline how possible SDK might look for a client.

The node implementation notes: 
* `node_api.proto` has to be updated with the two new methods:
    * `rpc GetGCSStream(GetGCSStreamRequest) returns (stream GetGCSStreamResponse) {}` where  
       `case class GetGCSStreamRequest(lastKnownAtalaObjectId: Option[AtalaObjectId])`  
       `case class GetGCSStreamResponse(objectGCS: AtalaObjectGCS)`  
       `case class AtalaObjectGCS(objectId: AtalaObjectId, objectGCS: GCS)`.
    
    * `rpc GetAtalaObject(GetAtalaObjectRequest) returns (GetAtalaObjectResponse) {}` where  
      `case class GetAtalaObjectRequest(objectId: AtalaObjectId)`  
      `case class GetAtalaObjectResponse(objectId: AtalaObjectId, objectOperations: List[OperationOutcome])`  
      `case class OperationOutcome(val operation: Operation, val status: ConfirmedStatus)`,  
       perhaps, in the actual implementation `OperationOutcome` will resemble `AtalaOperationInfo` with an additional field `Operation`,  
       also a new class for Atala object with operations together with statuses will be introduced.
* The only change needed on the database is to add `gcs` column in the `atala_objects` table,
  and to add a corresponding a lazy field to `AtalaObjectInfo` with some additional refactoring.
* Streaming will be implemented like `MessageNotificationService` in `connector` 
  (which doesn't seem a proper role model but perhaps the best we have).
* `operationsToGCS` function might be implemented like this:
```scala
def operationsToGCS(operations: List[OperationOutcome]): GCS = {
  def operationToFilters(op: OperationOutcome): SubscriptionFilter = op.operation match {
    case UpdateDIDOperation(did, _, _, _, _) => DidOperationFilter(DidUpdateTag, did, Some(op.status))
    // the similar code here
  }
  new GCS(operationToFilters(op), P) // P here is a parameter of GCS, will be specified during the implementation
}
```
* As an optimization we could respond with a batch of several Atala objects `GCS` in `GetGCSStreamResponse`.
  Also, we could add an extra `GCS` built for all operations from all the objects in the batch, 
  then a client could quickly skip bigger batches of irrelevant operations.

Sketch of a possible low-level client SDK interface:
```scala
abstract class NodeSubscriber(nodeService: NodeService) {
  var filters: Set[SubscriptionFilter]
  
  def subscribe(filters: Set[SubscriptionFilter]): fs2.Stream[OperationOutcome]
  def removeFilter(filter: SubscriptionFilter): Unit
  def addFilter(filter: SubscriptionFilter): Unit
  def unsubscribe(): Unit
}
```

`subscribe` is the most difficult method to implement, and 
possibly, the trickiest part is to achieve a decent level of parallelism on a reconnection, 
at the same time preserving the linear order of operations.  
The obvious option could be to send `GetAtalaObjectRequest` sequentially, waiting for the corresponding response.

Interface above is a low-level and is unlikely to be used directly by an SDK user 
because the main use case is to be notified about DID document updates and operations with credential issuance/revocation.  
The high level interface might look like:
```scala
abstract class DidEventsSubscriber(nodeService: NodeService, val didSuffix: String) {
  var filters: Set[SubscriptionFilter]
  
  // All APPROVED operations related to the passed DID
  def didOperations(): fs2.Stream[Operation]
  
  // All APPROVED DID updates (without issuance and revocations)
  def didDocumentUpdates(): fs2.Stream[Operation]
}
```

Implementation of `DidEventsSubscriber` will use an implementation of low level `NodeSubscriber`.

## Advantages, disadvantages and alternatives
Several advantages of the suggested approach are:
* No persistent data on a node, no IO overhead
* As GCSs are sent to clients, clients' DIDs are kept private and can't be exposed easily
* In-memory data size is `O(N)`, where `N` is number of clients, as only connections with subscribers are held
* Smaller load on a node, as all CPU consuming tasks are performed by clients
* Load on a node can be easily reduced by setting up reverse proxies in front of the node  
Disadvantages:
* Communication overhead on unrelated to a client GCSs notifications
* Requires implementation and maintenance of the code on both sides: node and client SDK

Alternative obvious approach could be sending a client's GCS to a node in order to reduce communication overhead.
In this case, a node would have to merge all received GCSs to some advanced structure
to be able to look up relevant subscribers quickly for every confirmed Atala block.
But this approach would require some sophisticated structures to be implemented, 
what will cause huge amount of data kept in memory and much bigger load on a node due to searches in the structure.  
Another disadvantage is that such structure can't be easily updated with new filter: it would require `O(K * F)`
operations on every update on every subscriber,
where `K` is the number of filters a client is interested in, `F` - time which needed to insert one hash in the structure
(something logarithmic).

Next idea which might come into mind: not use GCS, explicitly send filters to a node, 
keep all the information in SQL to reduce size of in-memory stored data and to perform searches 
of relevant subscribers quicker. But this makes client exposed to the man-in-the middle attack (message encryption needed),
still might cause hig load on a node, and moreover cause IO load which could be even worse than CPU load.

All in all, after long consideration the suggested approach was deemed as a good trade-off between
higher performance and implementation complexity.
