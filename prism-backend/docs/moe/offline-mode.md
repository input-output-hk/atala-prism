# PRISM Offline Mode
This document describes an approach on how to keep some PRISM functionality while our Cardano node is isolated from the rest of the world.

## Context
In the context of Ministry of Education (MoE) deployment, the Ethiopian government can take the internet down for several weeks. When this happens services run by the government can still communicate with each other, but no service inside Ethiopia can communicate to the outside world.

Since all PRISM services will be run by the government (see on-premise deployment design document for more details), it means that PRISM services can still communicate with each other (let it be Connector/Console/PRISM Node/Cardano Node etc). This allows clients to keep using the system for most operations, including the interactions with the PRISM node. As our Cardano instance will be disconnected from other Cardano nodes, no new transactions will be published on Cardano neither we will be getting new operations from Cardano.

PRISM Node depends on the Cardano network to push changes to it (creating a DID, publishing credentials, etc), as well as to pull such changes from it once new Cardano transactions get confirmed.

## Goal
The goal of this document is to analyze the current state of affairs in respect to the MoE requirements and propose a way to streamline users' experience with PRISM in such **offline mode**.

The gathered requirements on offline mode are as follows:
- Users should be able to create and use unpublished DIDs.
- Users should be able to create contacts.
- Users should be able to establish connections with each other via connection tokens, including recently generated ones.
- Issuers should be able to create credentials and share them with contacts.
- Verifiers should be able to verify credentials that have already been published on Cardano, but the resulting verification status should be tied to a timestamp of the latest confirmed Cardano block.
- Verifiers should be able to validate signatures of credentials that have not been published on Cardano yet. The resulting verification status should clearly reflect that the credential can not be verified yet, but was indeed issued by the correct issuer (assuming that the issuer's key is published and confirmed by PRISM Node).

Note that users will not be able to publish new DIDs, update existing DIDs, revoke confirmed credentials or publish new credentials.

## Current State
Let's analyze the requirements and identify what is support by PRISM as is:
- *Creating and using unpublished DIDs*. Already supported: unpublished DIDs can be generated client-side without any outside connectivity. The generated unpublished DID can be used for authorization, but not for issuing credentials (as the used key must be issuing key and not the master key).
- *Creating contacts*. Already supported: creating a contact only involves management console and connector backends.
- *Establish connections*. Already supported: establishing connections only involves connector backend.
- *Creating and sharing credentials*. An issuer can create a credential and generate Merkle tree root with proofs without invoking any PRISM services. Sharing the credential, however, involves publishing credential issuance batch on PRISM Node first. In response, we get the id of the transaction that contains our issuance batch operation. The issuer can then monitor the status of the operation to decide when is the right time to share the credential with the holder. This approach has some shortcoming that we need to address:
    - If the initial transaction fails, PRISM Node will try to resubmit it resulting in a new transaction with a new id. This is especially important in offline mode as the transaction will be failing constantly. We need to figure out a way to identify operations differently.
    - Since we will not have transaction id anymore, we will need an alternative way to check operation status.
- *Verifiers should be able to verify published credentials*. Mostly supported: PRISM Node holds the confirmed state in database and will be able to respond while Cardano is offline. However, the response is not timestamped with the latest Cardano block processed by PRISM Node.
- *Verifiers should be able to validate signatures of unpublished credentials*. Already supported: if holder shares an unpublished credential with the verifier, the verifier can easily check the signature on their side and decide how to treat the credential accordingly.

## Proposal
The analysis of the current state of affairs has shown that most of the required functionality is already supported by PRISM. Let's tackle the remaining couple of problems:
- *A new way to identify operations*. We already have a database table `atala_objects` that holds all submitted Atala objects along with their `atala_object_id`, but each object can actually contain multiple operations so Atala object id is not usable for identifying Atala operations. Let's introduce a new table `atala_operations` with `atala_operation_id` primary key. Since Slayer protocol does not allow two identical Atala operations to be posted, we can use operation's SHA256 hash as the identifier. We will also include a foreign key for keeping track of the Atala object the operation belongs to.
- *A new way to check operation status*. We propose to implement a new RPC `GetOperationState` in PRISM Node that, given an Atala operation id, would respond with one of the following statuses:
  - Unknown: the operation is not a recognized Atala operation.
  - Pending submission: the operation was received by the node, but has not been sent to the blockchain yet.
  - Awaiting confirmation: the operation has been sent to the blockchain, but has not been confirmed yet.
  - Confirmed and applied: the operation has been confirmed in the blockchain and was successfully applied to the PRISM Node's state. The response will also include transaction id.
  - Confirmed and rejected: the operation has been confirmed in the blockchain and was deemed invalid according to the PRISM Node's state. The response will also include a reason for rejection.
- *Timestamped responses*. We propose to add a new timestamp field to `GetDidDocument`, `GetOperationState`, `GetBatchState` and `GetCredentialRevocationTime` responses. The timestamp will represent when was the last time PRISM Node processed a new Cardano block (does not matter if it did or did not contain Atala objects). We can update it at the same time we update the last processed block and store it in the existing `key_values` table.

### Queue for requests
Aside from these functional requirements, we also need to be able to hold all unconfirmed operations in PRISM Node to publish them at a future date once Cardano comes back online. As was mentioned above, we persist all incoming Atala objects in `atala_objects` table, which is exactly what we want: a list of objects (containing Atala operations) that have not been published yet.

Whether these Atala objects are going to be published correctly once Cardano comes back online still remains, however. We already have a mechanism for retrying old transactions, so occasionally we will try to resubmit transaction to the wallet. One thing we can also try is submitting transactions without TTL as was suggested by Adrestia team [in Slack](https://input-output-rnd.slack.com/archives/C819S481Y/p1621355796019700).

### Batching
Although not directly related to the proposal, we think it would be also worth mentioning batching capabilities for PRISM Node. Currently, we create a Cardano transaction per each Atala operation. We can improve the process by grouping multiple operations in a single Atala object. This will allow us to save on Cardano fees and increase system's throughput.

The following must be considering for the implementation:
- Since we do not have a defined order of Atala operations (unlike, for example, Ethereum where nonce represents the order of transactions from a given address), we can batch transactions in the order of their receival. This should be revisited if/when we introduce an ordering mechanism.
- Until we have an ordering mechanism, DID key revocation operations cannot be safely sent along with operations that use that key within the same block.
- PRISM Node allows submitting full Atala objects as well (as opposed to individual operation). We should flatten such objects into individual transactions, but also make sure to include all the operations from an object into one transaction.

Note that the solution to problems discussed above will involve introducing deep introspection of incoming operations (something that Node does not do until the operation gets confirmed). Hence, this effort can be paired with pre-apply checks.
