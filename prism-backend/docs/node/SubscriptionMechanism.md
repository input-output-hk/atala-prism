## Subscriptions and their use cases
Before jumping to description of details of subscription mechanism, we will describe some sensible
use cases which might be useful for various parties of the system.

There are several roles, which might want to subscribe to some channels:
* **DID owner** - a user who reveals their identity in order to receive only related to them events.
A Prism node leverages this information to filter out all events and publish to the channel only related ones.
* **Anonymous** - 
  a user which doesn't reveal their identity, usually due to either they want to listen to some general information or
  it's impossible to identify related to user events without revealing some private information of the user.
  Another desire could be unwillingness to reveal own identity.

Subscription channels and their use cases:
1. **Anonymous** wants to be notified about _all CreateDID_ operations, which have been approved in the Cardano network.
   * Wallet user who use several devices wants to keep all devices up to date,
   if an operation happened on any of the devices.  
   To achieve it they check if every created DID is owned by them.

2. **DID owner** wants to be notified about _UpdateDID_ operation related to the DID, which have been approved in the Cardano network.
   * Wallet user who use several devices wants to keep all devices up to date.
   * Issuer wants to know about creation/revocation of keys which happens on another node.

3. **DID owner** wants to be notified about _IssueCredentialBatch_ ans _RevokeCredentials_ operations signed with one of the keys of the DID, and approved in the Cardano network.
   * Issuer wants to know about all credentials signed with their issuing key,
     which could have been shared with some trusted party.
   * Issuer wants to know about all revoked credentials signed with their revocation key,
     which could have been shared with some trusted party.

4. **Anonymous** wants to be notified if a _particular_ credential was revoked, and it was approved in the Cardano network.
   * Verifier is interested if a credential which was provided by a holder is still valid.

5. **Anonymous** wants to be notified about _all_ AtalaObjects after they are confirmed in the Cardano network.
     Status might be either _APPROVED_ or _REJECTED_.
   * Anybody wants to know about some other events confirmed, and those events are not covered by the above subscriptions.

6. **DID owner** wants to be notified about _own_ AtalaOperations after they are confirmed in the Cardano network.
     Status might be either _APPROVED_ or _REJECTED_.
   * Issuer wants to know when their published operation is actually confirmed: 
     for instance, a credential issuance or its own DID update.
   * Wallet wants to know when published update/creation of its DID is actually confirmed.

7. **Anonymous** wants to be notified about _particular_ AtalaOperation's status: 
   _PENDING_, _CANCELLED_, _REJECTED_, _IN_BLOCK_, _CONFIRMATION_LEVEL n_, _APPROVED_.
   * Wallet wants to show a user status of operation in real time to make a wallet interface more responsive.

## Subscription mechanism details
I suggest the following flow for subscriptions:
1. a subscriber initiates a subscription with list of _event types_ he is interested in
2. a node allocates a _subscription token_, saves a list of event types for it, and send it to subscriber
3. a subscriber saves a subscription token
4. a node notifies a subscriber about all relevant events (which match one of the event types)
5. a subscriber saves the last event which they have seen

If a subscriber reconnects (after being down):
1. they send corresponding subscription token and last seen event.
2. a node publish all relevant events which happened while a subscriber was down
3. a node keep publishing real time events

A subscriber can unsubscribe, then a node vanishes a corresponding subscription token and event types list.

## Messages content
