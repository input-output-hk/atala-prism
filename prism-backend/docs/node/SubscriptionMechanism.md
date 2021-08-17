## Subscriptions and their use cases
Before jumping to description of details of subscription mechanism, we will describe some sensible
use cases which might be useful for various parties of the system.

There are several roles, which might want to subscribe to some channels:
* **DID owner** - a user who reveals their identity in order to receive only related to them events.
A Prism node leverages this information to filter out all events and publish to the channel only related ones.
* **Anonymous** - 
  a user which doesn't reveal its identity, usually due to either it wants to listen to some general information or
  it's impossible to identify related to user events without revealing some private information of the user.
  Another desire could be unwillingness to reveal own identity.

Subscription channels and their use cases:
1. **Anonymous** wants to be notified about _all CreateDID_ operations, which have been approved in the Cardano network.
   * Wallet user who use several devices wants to keep all devices up to date,
   if an operation happened on any of the devices.  
   To achieve it, it checks if every created DID is owned by them.

2. **DID owner** wants to be notified about _UpdateDID_ operations related to the DID, which have been approved in the Cardano network.
   * Wallet user who use several devices wants to keep all devices up to date.
   * Issuer wants to know about creation/revocation of keys which happens on another node.

3. **DID owner** wants to be notified about _IssueCredentialBatch_ and _RevokeCredentials_ operations signed with one of the keys of the DID, and approved in the Cardano network.
   * Issuer wants to know about all credentials signed with their issuing key,
     which could have been shared with some trusted party.
   * Issuer wants to know about all revoked credentials signed with their revocation key,
     which could have been shared with some trusted party.

4. **Anonymous** wants to be notified if a _particular_ credential was revoked, and it was approved in the Cardano network.
   * Verifier is interested if a credential which was provided by a holder is still valid.

5. **Anonymous** wants to be notified about _all_ `AtalaOperation`s after they are confirmed in the Cardano network.
     An operation might be either _APPROVED_ or _REJECTED_.
   * Anybody wants to know about some other events confirmed, and those events are not covered by the above subscriptions.

6. **DID owner** wants to be notified about _own_ `AtalaOperation`s after they are confirmed in the Cardano network.
   An operation might be either _APPROVED_ or _REJECTED_.
   * Issuer wants to know when their published operation is actually confirmed: 
     for instance, a credential issuance or its own DID update.
   * Wallet wants to know when published update/creation of its DID is actually confirmed.

7. **Anonymous** wants to be notified about _particular_ `AtalaOperation`'s status: 
   _PENDING_, _CANCELLED_, _REJECTED_, _IN_BLOCK_, _CONFIRMATION_LEVEL n_, _APPROVED_.
   * Wallet wants to show a user status of operation in real time to make a wallet interface more responsive.

## Subscription mechanism approach
I suggest the following flow for subscriptions:
1. a subscriber initiates a subscription with list of event types it is interested in, so-called _filters_
2. a node allocates a _subscription token_, saves filters for the subscription token, and sends the subscription token to the subscriber
3. the subscriber saves the subscription token
4. the node notifies a subscriber about all relevant events (which match one of the filter)
5. the subscriber saves the last event which it has seen

If a subscriber reconnects (after being down):
1. it sends corresponding subscription token and the last seen event to a node
2. the node publishes all relevant events which have happened while the subscriber was down but after the last seen event
3. the node keeps publishing real time events

A subscriber can unsubscribe, then a node vanishes a corresponding subscription token and filters.

## Filters and related types
In this section, to give a reader better understanding of happening,
we will describe classes and types which cover mentioned in the first section cases.

As for now, the last (7th) case is omitted, and it will be implemented in future 
and the implementation will be covered in the next versions of the document.

We start with some auxiliary and abstract definitions.
```scala
trait OperationStatus
object AppliedOperationStatus extends OperationStatus
object RejectedOperationStatus extends OperationStatus

abstract case class SubscriptionFilter(statuses: Array[OperationStatus])
```

`statuses` determines which operation outcomes a filter matches.


```scala
class DidOwnerAnyFilter(val didSuffix: DIDSuffix,
                        override val statuses: Array[OperationStatus]) extends SubscriptionFilter(statuses)
case class UpdateDidFilter(override val didSuffix: DIDSuffix,
                           override val statuses: Array[OperationStatus]) extends DidOwnerAnyFilter(didSuffix, statuses)
case class CredentialsIssuanceFilter(override val didSuffix: DIDSuffix,
                                     override val statuses: Array[OperationStatus]) extends DidOwnerAnyFilter(didSuffix, statuses)
case class CredentialsRevocationFilter(override val didSuffix: DIDSuffix,
                                       override val statuses: Array[OperationStatus]) extends DidOwnerAnyFilter(didSuffix, statuses)
```
These classes describe filters which match events produced by some DID. 
`DidOwnerAnyFilter` match any of such events, other ones match more specific events.


```scala
class AnonymousAnyFilter(override val statuses: Array[OperationStatus]) extends SubscriptionFilter(statuses)
case class CreateDidFilter(override val statuses: Array[OperationStatus]) extends AnonymousAnyFilter(statuses)
case class RevokeCredentialFilter(credentialHash: SHA256Digest, 
                                  override val statuses: Array[OperationStatus]) extends AnonymousAnyFilter(statuses)
```
These classes describe filters which match events, regardless the fact who produced related operation.
`AnonymousAnyFilter` match arbitrary such event, other classes match more specific events.

## Implementation details
This section will cover implementation details, in particular, the following topics:
* new subscription
* syncing up a reconnected subscriber with missed events
* publishing new events to subscribers
* unsubscription
* operations ordering

Firstly, let's clarify what "event" means and how it relates to "operation".
As we excluded from our consideration 7th case, we have almost one to one correspondence between "event" notion and "operation".
There are two directions for changes:
1. include in an event only relevant set of information, which include subset of data from an operation plus some extra useful information
2. make an event as operation plus some useful extra data

The first option would introduce new classes, and most importantly new classes to gRPC, however, will reduce amount of data sent.
The second option makes implementation easier and provide a subscriber the bigger amount of information, though, loses in message size.
Within the scope of this document I will imply that we go with the second option, and that "event" contains an "operation" fully, maybe with some extra data.
So will the initial implementation be, though it may be changed in the future.

Let's move on to the actual details. 
Firstly, we need to introduce SQL tables where all subscription specific data will be stored, let's describe them.

`subscriptions` table contains high level information about every subscription.
#### subscriptions table
* `subscription_token` - a randomly generated string which identifies one subscription, primary key
* `after_operation` - `AtalaOperationId` which corresponds to a last processed operation at the moment of the subscription. 
  This field serves as a "lower bound" of events which a subscriber will be notified about after reconnection, 
  if it didn't require previous ones explicitly.
* `operations_batching_allowed` - boolean meaning 
   if a subscriber is able to accept several events batched in order to
   reduce communication overhead caused by individual events sent over the communication channel separately

`subscription_filters` table contains information about filters and which subscription they are applied to.
#### subscription_filters table
* `subscription_token` - determines a subscription which subscription filter is applied to, foreign key to `subscriptions` table
* `statuses` - an enum which determines statuses a filter matches, `null` if any status matches
* `filter_type` - an enum corresponding to a one of 7 classes listed in the second section
* `did_suffix` - a DID suffix of operation, which a filter matches, `null` in case of anonymous filter
* `revoked_credential_hash` - credential hash from `RevokeCredentialFilter`, `null` if not applicable

### New subscription
Here is a flow of new subscription generation:
1. A subscriber sends a `NewSubscriptionRequest`:
```scala
case class NewSubscriptionRequest(filters: Array[SubscriptionFilter], allowOperationsBatches: Boolean)
```
2. A node generates `subscription_token` randomly, insert into `subscription_tokens` among with the last operation hash in `atala_operations` and `allowOperationsBatches`
3. Then node remove repeating and merge complementary filters, and insert them into `subscription_filters`
4. After that it responds to the subscriber `NewSubscriptionResponse`:
```scala
case class NewSubscriptionResponse(subscriptionToken: String, eventsAfter: AtalaOperationId)
```
5. After that, the subscriber actually opens a stream with events with request `GetSubscriptionEventsStream`:
```scala
case class GetSubscriptionEventsStream(subscriptionToken: String, eventsAfter: Option[AtalaOperationId])
```
and the node starts streaming relevant events to the subscriber.

Pay attention that a subscriber is allowed to specify arbitrary `eventsAfter`, even leave it `None`.
In the last case all relevant operations, which node has, will be sent to a subscriber.
However, this freedom might cause significant load on a node, especially in case of anonymous subscriptions,
as a node will have to send all operations.

We smoothly move to the next part.

### Sync up
The sync up happens when a subscriber connects to the node with `GetSubscriptionEventsStream`, 
regardless if it happens after `NewSubscriptionResponse` or after a subscriber being down.
In both of these cases we would like to sync up the subscriber with missed events if any.

To be able to implement this procedure we have to extend `atala_operations` table with three extra columns:
* `atala_operation_did_suffix` - DID suffix of an operation, `null` in case of `CreateDID`
* `atala_operation_type` - an enum corresponding to one of 4 types of operations
* `atala_operation_content` - serialised content of an operation needed to send an operation to a subscriber during sync up process.
   This field introduces data redundancy with `object_contet` from `atala_objects`, which will be eliminated later.

Having these additional columns the procedure will be:
1. Select all `subscription_filters` for `subscription_token`
2. Generate from them corresponding statement for `where` clause of `select` SQL statement: 
   every of the filters impose potential restrictions on `atala_operation_did_suffix`, `atala_operation_type` and `atala_operation_status`, 
filter clauses are connected with `OR`.
3. Select all relevant operations with the above clause. 
   `RevokeCredentialFilter` filter requires some extra care to filter out operations that don't contain a specific `credentialHash`.
4. Return all operations to the subscriber (possibly as a batch, if a subscriber allowed batching)
5. Go to a normal flow, when new processed operations are published to subscribers

### Publishing new operations
1. When an operation is processed by `BlockProcessingService.processOperation` either successfully or not,
we build a corresponding SQL statement to select matching `subscription_filters`.
This might be done in unambiguous way: every operation corresponds to set of filters which might match it.
2. Then we select all unique `subscription_token` from `subscription_filters` matching the SQL statement and pick ones,
that have active connection with the node (not being down).
3. Send the operation to the picked connections

### Unsubscription
If a subscriber isn't interested in events anymore, it sends `CancelSubscription`:
```scala
case class CancelSubscription(subscriptionToken: String)
```
and a node drops all related entries from `subscriptions` and `subscription_filters` tables.

### Operations ordering
In the previous subsections we already mentioned operations ordering: we have to send all operations happened after the last seen event,
and we have to provide them in the order of confirmation.

Having current database schema, it might be done but in really tricky way:
* join `atala_object_txs` table with `atala_objects`, 
* select only ones which go not earlier than a block containing last seen operation,
* sort by `(block_number, block_index)`,
* deserialize `object_content` and traverse all operations sequentially in the object
* match ones with subscription filters and send them to a subscriber

This seems to me extremely inefficient, and two main issues are that we can't order operations within an object without prior deserialization,
and we have to traverse them manually to match against filters.

However, it could be easily fixed by adding `atala_operation_seq_no` column to `atala_operations` and 
update it when a block is processed, among with `atala_operation_status`.
`operation_seq_no` is a global sequential number of operation in order of confirmation, which can be updated with PostgreSQL sequences.

After having that done, we could select and order all related operations as it was described in **Sync up** section, taking into account these columns:
* `atala_operation_seq_no`
* `atala_operation_did_suffix`
* `atala_operation_type`
* `atala_operation_status`
* `atala_operation_content`
