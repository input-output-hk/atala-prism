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

2. Verifier wants to be notified if a _particular_ credential was revoked, and it was approved in the Cardano network.
   Verifier is interested if a credential which was provided by a holder is still valid.

3. Wallet wants to be notified about _particular_ `AtalaOperation`'s status
   (_PENDING_, _CANCELLED_, _REJECTED_, _IN_BLOCK_, _CONFIRMATION_LEVEL n_, _APPROVED_),
   in order to show a user status of operation in real time to make a wallet interface more responsive.

The last case and similar ones will be out of the consideration of this document for now,
we will mostly focus on notifications about Atala operations.

## High level overview of the protocol
Suggested protocol is inspired by [BIP-157](https://en.bitcoin.it/wiki/BIP_0157) and [BIP-158](https://en.bitcoin.it/wiki/BIP_0158).

The core structure of this approach is Golomb Coded Set filter (GCS).
This structure is basically compressed set that supports element insertion and element existence test.
Testing of existence is probabilistic but the probability is close to 1.

The protocol in nutshell is that when a node receives an Atala block,
it computes GCS consisting of the block operations, send the resulting GCS to a client, 
the client tests if there is at least one element it is interested in, and if so it requests them.

Let's move on to more detailed description.  
Let's assume that a node has the list of subscribers, stored in memory,
and each of those has associated gRPC stream.
Then that will happen on new event:
1. when an event happens, a node computes a GCS for it, and saves the event and GCS in a persistent storage, 
   sends the GCS to all subscribers' streams
2. when a client receives a GCS, it checks if there are entries in the GCS which a client is interested in.  
   if there are any, in a separate connection a client requests an original event which the GCS was derived from, 
   otherwise saves the GCS as a last known one to a local persistent storage.
3. in case, if a client requests an event for GCS, a node responds to the client with the original event. 
   This message is sent to the separate connection, not to the associated stream.
4. upon receiving an event, a client handles it and saves a corresponding GCS as last known one

Pay attention, that an event in the 2nd step is a _flexible_ notion, 
and might be any kind of thing, for example, an Atala block, an Atala operation, or even
a node lifetime event (e.g. a node disconnected from the Cardano network).
Hence, what an event actually is depends on implementation, and in the first version it will be just an Atala block.

Another subject for discussion is whether we need to store all kind of events persistently. 
For example, a node disconnect event seems unimportant after a node connects back, 
but let's leave this question for future discussion.

Also, in the step 4th it depends on actual implementation who will respond to a client with events.
In the simplest case, it might be a node itself, however, 
in order to reduce load on a node, a reverse proxy or some kind of cache might be in front of the node.

Let's get back to our assumption about the subscribers list on the node, 
and describe how a client will actually connect to a node:
1. a client initiates a stream connection to a node sending a last known GCS if any
2. a node responds with GCSs from its persistent storage to the stream
3. a client filters out received GCSs, and requests corresponding events from the node in a separate connection(s)
4. a node responds with requested events
5. a client handles them, updating its last known GCS
6. after that, a client moves to the previously described flow

## Filters and related types
In this section we will outline _filter_ types, which a client leverages to specify which operations it's interested in.

We start with some auxiliary and abstract definitions.
```scala
sealed trait ConfirmedStatus
case object AppliedConfirmedStatus extends ConfirmedStatus
case object RejectedConfirmedStatus extends ConfirmedStatus

abstract class SubscriptionFilter(val status: Option[ConfirmedStatus]) {
  // Hashes for Bloom-filter
  def hashes: List[Long]
}

trait GCS {
  def exists(g: SubscriptionFilter): Boolean
  def insert(g: SubscriptionFilter): GCS
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

  override def hashes: List[Long] = {
    // Hashes tuple (operationTag, didSuffix), status is not hashed if `None`
  }
}

case class RevokeCredentialFilter(credentialHash: SHA256Digest,
                                  override val status: Option[ConfirmedStatus]) extends SubscriptionFilter(status) {
  override def hashes = {
    // Hashes a tuple ("RevokeCredentialFilter", credentialHash, status)
  }
}
```
These filters correspond to all cases from the first chapter.

The only questionable one is `DidOperationFilter(CreateDidTag, ... )` because it implies that 
we can freely generate a next expected DID from mnemonic and that all wallets generate DIDs sequentially.
If the latter can be required by the protocol, the former requires password input from a user.

## Implementation details
In this section we describe how suggest protocol will be integrated in the node codebase,
and outline how possible SDK might look for a client.

The node implementation notes: 
* `node_api.proto` has to be updated with the two new methods:
    * `rpc GetGCSStream(GetGCSStreamRequest) returns (stream GetGCSStreamResponse) {}` where  
       `case class GetGCSStreamRequest(lastKnownAtalaObjectId: Option[AtalaObjectId])`  
       `case class GetGCSStreamResponse(GCSs: List[AtalaObjectGCS])`  
       `case class AtalaObjectGCS(objectId: AtalaObjectId, objectGCS: GCS)`  
       `List` is used in `GetGCSStreamResponse` in order to response with a GCSs batch on a re-subscription to reduce network overhead.
    
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
  operations.foldLeft(emptyGCS)((gcs, op) => gcs.insert(operationToFilters(op)))
}
```
* If we assume that the most of the operations are Credential issuance, 
  we could introduce a little optimisation: to add in `GetGCSStreamResponse` 
  an extra GCS in order to skip most of the filter tests against GCSs in the list.

Sketch of possible client SDK interface:
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
