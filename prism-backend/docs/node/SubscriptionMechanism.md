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
1. **Anonymous** wants to be notified about _all CreateDID_ operations.
   * Wallet user who use several devices wants to keep all devices up to date,
   if an operation happened on any of the devices.  
   To achieve it they check if every created DID is owned by them.

2. **DID owner** wants to be notified about _UpdateDID_ operation related to the DID.
   * Wallet user who use several devices wants to keep all devices up to date.
   * Issuer wants to know about creation/revocation of keys which happens on another node.

3. **DID owner** wants to be notified about _IssueCredentialBatch_ operation signed by one of issuing key of the DID.
   * Issuer wants to know about all credentials signed with their issuing key,
     which could have been shared with some trusted party.

4. **DID owner** wants to be notified about _RevokeCredentials_ operation signed by one of revocation key of the DID.
   * Issuer wants to know about all revoked credentials signed with their revocation key,
     which could have been shared with some trusted party.

5. **Anonymous** wants to be notified if a _particular_ credential was revoked.
   * Verifier is interested if a credential which was provided by a holder is still valid.

6. **Anonymous** wants to be notified about _all_ AtalaObjects statuses after it's confirmed on the Cardano chain,
     which might be either _APPROVED_ or _REJECTED_.
   * Anybody wants to know about some other events confirmed, and those events are not covered by the above subscriptions.

7. **DID owner** wants to be notified about _own_ AtalaOperations statuses after it's confirmed on the Cardano chain, 
     which might be either _APPROVED_ or _REJECTED_.
   * Issuer wants to know when their published operation is actually confirmed: 
     for instance, a credential issuance or its own DID update.
   * Wallet wants to know when published update/creation of its DID is actually confirmed.

8. **Anonymous** wants to be notified about _particular_ AtalaOperation's status: 
   _PENDING_, _CANCELLED_, _REJECTED_, _IN_BLOCK_, _CONFIRMATION_LEVEL n_, _APPROVED_.
   * Wallet wants to show a user status of operation in real time to make a wallet interface more responsive.
