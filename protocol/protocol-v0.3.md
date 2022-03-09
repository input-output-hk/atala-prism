<!-- This is meant to be part of a larger document -->

\newpage

# Slayer v3: Scaling without a second layer

## Motivation

The goal of the protocol that we describe in this document, is to construct a scalable, decentralised and secure 
identity system. The system must provide the following features:

1. Allow the decentralised creation of self certifiable identifiers. This is, any person can create an identifier
   without the need of coordination with, or permission from any external authority. Only the creator (controller) of 
   the identifier can prove his ownership through the use of cryptographic techniques.
2. Allow controllers to update the state of their identifiers.
3. Given an identifier, allow anyone to obtain its current state.
4. Allow asserting claims using the identifiers.

In particular, we will refer to these identifiers as **D**ecentralised **ID**entifiers (DIDs). There is a working group 
in W3C specifying the nature of these entities. For the purpose of this document, we will simplify their definition 
and declare that a DID is a string, which is associated to a document (DID Document). A DID Document declares the 
state of its associated DID. The first DID Document declares the *initial state* associated to a DID. This document 
contains cryptographic keys that allow controllers to prove their ownership over the DID, and to update the associated 
document. 
The DID Document can also contain other data, such as, URLs, referring to external information about the associated DID
controller. The document can also be updated. In our construction, the identifier (DID) has the form 
`did:prism:hash_initial_DID_Document`, making our DIDs, self-certifiable. That is, given a DID and its initial DID
Document, anyone could verify that the document is really associated to the identifier by comparing the identifier to
the hash of the document.

In previous versions of our protocol, we followed the ideas behind Sidetree. 
Sidetree is a protocol developed by Microsoft, and is currently being specified in a Working Group inside the
Decentralised Identity Foundation. In a simplified explanation, independent nodes run the Sidetree protocol using an 
underlying blockchain as base layer. Each node can post file references in the metadata (or equivalent) field the
blockchain's transactions. The references point to files in a publicly accessible content addressable storage (CAS) 
service. The files contain the following events:

 - Create DID: An event that declares the creation of an identifier, and declares its initial DID Document. 
 - Update DID: An event that allows adding/removing data to the associated DID Document.
 - Deactivate DID: An event that declares that the DID is not usable anymore.
 
All events (except for DID creation ones), are signed by adequate keys to prove validity. The current state of a DID
Document is computed by taking the initial state (provided on DID creation), and applying the events found in files 
referenced on the underlying blockchain following the order in which they appear.

With respect to claims, there is [a variation of Sidetree](https://hackmd.io/tx8Z0mIRS-aK84Gx4xIzfg?view) that allows
DID controllers to sign statements and post hashes of them as events in the protocol files (the ones referenced by
blockchain transactions). This action, allows to timestamp the statement assertion while not revealing the statement 
content. The variation also allows the protocol to revoke an already asserted statement, also timestamping that 
event by adding it to a file.

The feature of `batching` events in files, and posting only references on-chain, allows a considerable scalability 
performance. However, we can observe some drawbacks related to this approach:
1. Data availability problems (referenced files may not be available). This leads to the inability to obtain consensus
   about the order in which events occurred in the past. Furthermore, it makes it difficult to know if certain event 
   were ever valid at all. This allows a situation known as ["late publication"](./late-publish.md), that could affect 
   certain use cases (e.g. DID transferability).

   Late publication also represents a potential change to the "past" of the system state.
2. Even though batching increases events throughput, it comes with the need of an anti spam strategy to avoid garbage
   events to saturate the system. This observation makes us think that the existence of batching does not necessarily 
   imply lower operation costs or lower fees for users.
3. The problems related to data availability lead to not trivial implementation considerations.

This document explores some changes to the previous version of our protocol (0.2), in order to balance better 
performance (in terms of event throughput) while avoiding the issues related to data availability.

## Changes proposed

### Credential issuance

[In version 0.2](./protocol-v0.2.md), we describe an issuance operation that, in a simplified way, contains:

``` scala
IssueCredential(
  issuerDIDSuffix: ...,
  keyId: ...,
  credentialHash: ...,
  signature: ...
)
```

Following Sidetree's approach, one would construct a file, `F1`, of the form

```scala 
IssueCredential(issuerDIDSuffix1, keyId1, credentialHash1, signature1)
IssueCredential(issuerDIDSuffix2, keyId2, credentialHash2, signature2)
···
IssueCredential(issuerDIDSuffixN, keyIdN, credentialHashN, signatureN)
```

and we would post a transaction containing the hash of `F1` on chain while storing `F1` in a CAS.

However, it is reasonable to believe that, an issuer, will produce many credentials in batches. Meaning that each issuer
could create a file where all the operations will be signed by the same key. This would lead to a file of the form:

```scala
IssueCredential(issuerDIDSuffix, keyId, credentialHash1, signature1)
IssueCredential(issuerDIDSuffix, keyId, credentialHash2, signature2)
···
IssueCredential(issuerDIDSuffix, keyId, credentialHashN, signatureN)
```

Given that all the operations would be signed by the same key, one could replace the `N` signatures and occurrences of
the `issuerDIDSuffix` and `keyId` by one, leading to a file with a single operation of the form:

``` scala
IssueCredentials(issuerDIDSuffix, keyId,
   credentialHash1,
   credentialHash2,
   ···
   credentialHashN,
signature)
```

Now, at this point, one could ask, could we replace the list of hashes for something shorter? The answer is, yes. 

An issuer could take the list of credential hashes `credentialHash1, credentialHash2, ..., credentialHashN` and compute
a Merkle tree with them. Obtaining one root hash and `N` proofs of inclusion.

```scala
MerkleRoot,
(credentialHash1, proofOfInclusion1),  
(credentialHash2, proofOfInclusion2),
···
(credentialHashN, proofOfInclusionN),
```

Now, the issuer could simply post this operation on the metadata of a transaction:

```scala
IssueCredentials(issuerDIDSuffix, keyId, merkleRootHash, signature)
```

and share with each corresponding holder, the pair `(credential, p)` where:

- `credential` is the credential to share and,
- `p` is the proof of inclusion that corresponds to `credential`  

By doing this, a holder could later share a credential to a verifier along with the proof of inclusion. The verifier 
would perform a modified version of the verification steps from version 0.2 of the protocol. See last section for the 
formal description.

A remaining question is, why would we want each credential operation separate in the first place? The answer is that, 
our intention is for the issuer to be able to detect if an unauthorized credential is issued. So, does this change 
affect that goal? Our argument is that, it does not. In order to detect that a credential was issued without 
authorization, the issuer needed to check each `IssueCredential` operation posted and check for those signed by his keys,
then compare those to the ones in his database and make a decision. With the proposed change, the issuer would perform 
the same steps, he would check all the merkle tree hashes posted with his signature and compare if that hash is 
registered in his database.

In conclusion, this change could allow us to remove the need of external files for issuing credentials and allow 
credential issuance to be fully performed on-chain using Cardano's metadata.

### Credential revocation

The proposed protocol changes that we will describe below, do not scale the throughput of credential revocations as much
as the increase we added for issuance. However, we could argue that credential revocation should be a type of event with
low throughput demand in comparison.

If we analyse revocation scenarios, we could see some special cases:

- The issuer detected a merkle root that is not authorised. In such case, he could revoke the full represented batch with
  a small operation of the form:
  ```scala
  RevokeCredentials(
    issuanceOperationHash,
    keyId,
    signature
  )
  ```
  The associated DID is the one from the referred `IssueCredential` operation.
- Another situation is when an issuer would like to revoke specific credentials _issued in the same batch_. For this 
  case, we could see the use of an operation:
  ``` 
  RevokeCredentials(
     issuanceOperationHash,
     keyId,
     credentialHash1,
     credentialHash2,
      ···,
     credentialHashK,
     signature
  )
  ```

Each hash adds approximately 32 bytes of metadata, which adds very little fee. See [fee](#fee-estimations) for more 
details on fees.

Now, if the issuer needs to revoke credentials issued in different batches, he could post independent transactions per
batch. Compared to version 0.2 of this protocol, we can revoke batches of credentials with a single operation in certain
cases. Before, we needed one operation per credential to be revoked.

We explored the idea of having an RSA accumulator per credential type. Our preliminary research makes us believe that 
the approach may require the use of big prime numbers, and the accumulator may occupy a few kilobytes of space. We 
decided to leave out the idea for a future version and request the suggestions from a cryptographer.

### DID creation

In order to create a DID, in version 0.2, we have to post a `CreateDID` operation on-chain. Sidetree allows to scale the
amount of DIDs to create by the use of batches. However, they also provide a 
[long-form](https://identity.foundation/sidetree/spec/#long-form-did-uris) that allows to use a DID before it is 
published. The long form also allows to never publish a DID if it is never updated.

We propose, for this protocol version, to leave DID creation operations as optional, just for the purpose of 
timestamping. This is:

- The initial DID could be posted on-chain as in version 0.2, or 
- It could have a long format that describes its initial state and no event will be published. The format could, 
  informally be thought as
   
  ```text
  did:prism:hash_initial_DID_Document?initialState=InitialDIDDocument
  ```
  The initial DID document can be validated by the DID suffix. 
- Another alternative is that many DIDs created by the same entity, could construct a Merkle tree (as we propose for
  credential hashes) and post the hash of the root on-chain for later timestamping proving.

In Sidetree's slack, we received the feedback that they see this as an evolution they plan for the protocol with some 
differences from our proposal.

- They do not care about batched creation timestamping as we propose.
- During the first update operation, they would like to post the initial DID state along with the update operation. 
  They prefer to store the initial state in the CAS rather than sharing a longer identifier on every interaction.
  We could leave this as two variations of the first update operation in our protocol, but there are considerations to 
  have.
    1. Not publishing the initial state leads to smaller metadata to add to the first update transaction. It also allows
       more privacy. One drawback is that invalid update operations would not be "prunable" until the associated DID is
       resolved for the first time. This could be mitigated by associating DIDs to addresses as mentioned in [other 
       ideas](./protocol-other-ideas.md).
    2. In the variation where the initial state is posted along with the first update, we would have bigger metadata 
       use. We should be careful to not exceed reasonable metadata size. See 
       [metadata considerations](#metadata-usage-concerns) for more comments on metadata usage.
  
For simplicity, we incline for option 2. As it would require fewer changes in the way we process operations. Currently,
when the node finds a new operation, it applies the state change right away. If we do not post the initial DID state, we
would need to process operations differently, as DID update operations would have no initial state to be applied upon.
Similarly, we need to have the DID state at the time of credential issuance and revocation. We should allow to publish a
DID during the first issuance operation too. Note that we won't need to publish a DID during the first revocation, because
the first revocation must occur after an issuance event. Hence, we will request the signing DID to already be published
by this point. 

In practice, we have implemented the above two cases through "on-chain batching", meaning that we allow users to publish
a sequence of events in a single blockchain transaction. In order to publish a DID during its first update, the user can
post the `CreateDID` event and the `UpdateDID` events in the same underlying transaction. Similarly, the user can also 
publish a `CreateDID` event along with an event to issue a batch of credentials. Any other combination of events can be
published too, the only restriction is for the entire sequence to be small enough too fit in a single transaction
metadata field. 
  
The use of a long format would enable us to create DIDs without the need of batching or any on-chain event whatsoever. 
This leads to unbound throughput for DID creation. 

*NOTE:* Even though there does not seem to be a maximum value for DID length, we should be aware of such discussions in 
DID core or similar groups. We have been warned that QR codes would not be good for DIDs with "big" long form. we could
evaluate compressing the data inside the QR and decompressing it at the receiving end.

### DID updates

This is the only operation that cannot be scaled on-chain in an easy way. The main reason is that update operations are
expected to be publicly available. This implies that a commitment (e.g. hash, merkle tree, RSA accumulator, etc.) is not 
enough for the users of the protocol. Nodes need the actual update data.

A question we may have is, how often do we expect an update to occur? If this is not a recurrent operation, we should
consider leaving it as an on-chain operation.

Alternatively, we could consider adding a permissioned batching service. This is:
- IOHK (and selected actors) could have a DIDs that can sign authorised batches.
- Some drawbacks are the complexities added by a missing trust model on who can batch update events, and also 
  complexities to handle data availability problems.

Originally, we were thinking about having permissioned batches for every type of operation. In the worst case scenario,
it now seems that we would only require batches for DID updates.
We should keep evaluating if this is needed for this version.

Another option could be to allow "on-chain batching". Note that we have a bit less than 16Kb for transaction metadata.
Consider that updating a DID consists on:

- Adding a key: requires a keyId and a key.
- Removing a key: requires a keyId.
- Adding/removing a service endpoint: an id and the endpoint when adding it.

If we imagine small update operations we could allow users to cooperate and batch up updates in a single ADA 
transaction.

- Actors could take periodic turns to distribute fee costs. 
- We could ask a cryptographer if there is any signature aggregation scheme that could help to reduce metadata size.
- We would like to refer the reader to the [metadata considerations](#metadata-usage-concerns) section to be aware of
  potential problems with "on-chain" batching.

## Fee estimations

According to [Shelley documentation](https://github.com/input-output-hk/cardano-ledger-specs/blob/master/shelley/design-spec/delegation_design_spec.tex#L1999),
there is a maximum transaction size. The documentation also states the following about fees:
 
> The basic transaction fee covers the cost of processing and storage. The formula is
> ```
> a + bx
> ```
>
> With constants `a` and `b`, and `x` as the transaction size in bytes.

The constants, according to [Shelley parameters](https://hydra.iohk.io/build/3670619/download/1/index.html) are:

```json
    "maxTxSize": 16384,
    "maxBlockBodySize": 65536,
    "maxBlockHeaderSize": 1100,
    "minFeeA": 44,
    "minFeeB": 155381,
```

Interestingly enough, the values `a` and `b` are inverted (a typo in the documentation already reported and confirmed).
From the above data, the maximum transaction fee that could be created is

```scala
maxFee = minFeeB + maxTxSize*minFeeA = 155381 + 16384*44 = 876277 lovelace = 0.876277 ADA
```

which represents a 16 kilobyte transaction.

Let us estimate fees per operation type. We will add extra bytes in our estimations due to the [metadata scheme enforced
by Cardano](https://github.com/input-output-hk/cardano-ledger-specs/blob/master/shelley/design-spec/delegation_design_spec.tex#L4547). 

We will assume:

- A base transaction size (i.e. without the metadata) of 250 bytes.
- A signature size of [75 bytes](https://crypto.stackexchange.com/questions/75996/probability-of-an-ecdsa-signature/75997#75997)
- A hash size of 32 bytes.
- A keyId size of 18 bytes.
- A key size of 32 bytes.
- A DID suffix of 32 bytes.

Given the above, we could estimate:

- DID creation without publishing would have cost of 0 ADA and uses no metadata nor transactions. 
- The old DID creation (publishing the operation) with two keys (one Master and one for Issuing) would have:
  - At least two key ids (2 x 18)
  - At least two keys (2 x 32)
  - We currently also have a signature (75)
  Adding the base transaction, we could overestimate this with a 400 bytes transaction, leading to:
     ```scala
         minFeeB + 400*minFeeA = 155381 + 400*44 = 172981 lovelace = 0.172981 ADA
     ```
- DID "batched" timestamping would have an operation identifier and a Merkle root hash. 
  We could overestimate this with a 300 bytes transaction.
  
    ```scala
    minFeeB + 300*minFeeA = 155381 + 300*44 = 168581 lovelace = 0.168581 ADA
    ```
  
- If we imagine a credential issuance operation (where the issuing DID was already published), we have:
  - an issuer DID suffix (32)
  - a signature (75)
  - a keyId (18)
  - a Merkle root hash (32)
  Overestimating, this leads to 450 bytes of metadata, leading to an issuance fee of:

  ```scala
  minFeeB + 450*minFeeA = 155381 + 450*44 = 175181 lovelace = 0.175181 ADA
  ``` 
  
  If we need to consider the data to publish the DID during the issuance operation, we could add:
  - 2 key ids (2 x 18)
  - 2 keys (2 x 32)
  Meaning that we add 100 bytes, leading to:
  ```scala
    minFeeB + 550*minFeeA = 155381 + 550*44 = 179581 lovelace = 0.179581 ADA
  ``` 
- For revocation, we could analyse the cost per type of revocation.
  - If the issuer revokes the full batch of credentials, it looks reasonable to estimate a similar cost that the 
    issuance operation, i.e. ~0.175181 ADA.
  - If the issuer needs to revoke selective credentials from a single batch, we could expect a fee similar to the one
    before with an addition of 2000 lovelace (32 bytes * 44 lovelace/byte + encoding bytes) 
    per credential hash. This represents 0.0002 extra ADA per revoked credential.
- DID updates would require a more variable estimation. Sidetree 
  [suggests](https://identity.foundation/sidetree/spec/#default-parameters) a maximum of 1 kilobyte for the size of an 
  update operation. In comparison, this would represent an approximate of:
  ```scala
    minFeeB + 1024*minFeeA = 155381 + 1024*44 = 200437 lovelace = 0.200437 ADA
  ``` 

We could apply some optimizations in exchange for making a slightly more complex protocol.
See [related work](./protocol-other-ideas.md).

### Metadata usage concerns

Even though ledger rules allow for transactions with big metadata, we should be aware that this is not a guarantee that 
the network will accept them. For example, Bitcoin transactions with more than one `OP_RETURN` output are valid 
according to consensus rules, however, they are not considered "standard" transactions and nodes do not tend to
forward them [1](https://medium.com/@alcio/an-inefficient-use-of-bitcoin-16281b975cae#:~:text=Transactions%20with%20multiple%20OP_RETURN%20outputs,by%20peers%20on%20the%20network.).
[2](https://www.frontiersin.org/articles/10.3389/fbloc.2019.00007/full). We raise this observation to motivate the most 
efficient use of metadata bytes. 
It may be reasonable to adopt a conservative design principle of "the smaller, the better".

## Formal changes

In this section we would like to summarise the protocol with respect to a more formal definition of the operations and
node state.

## Node State

Given that we will now batch credential issuance and also optionally timestamp DID creation batches,
we will update our node state definition. Let us start with type definitions:

```scala
// Abstract types
type Key  
type Hash
type Date

// Concrete types
type KeyUsage       = MasterKey | IssuingKey | AuthenticationKey | CommunicationKey
type KeyData        = {
  key: Key, 
  usage: KeyUsage, 
  keyAdditionEvent: Date,      // extracted from the blockchain
  keyRevocationEvent: Option[Date] // extracted from the blockchain
}

type DIDData        = {
  lastOperationReference: Hash, 
  keys: Map[keyId: String, keyData: KeyData]
}

type CredentialBatch = {
  issuerDIDSuffix: Hash, 
  merkleRoot: Hash,
  batchIssuingEvent: Date,            // extracted from the blockchain
  batchRevocationEvent: Option[Date]  // extracted from the blockchain
}

// Node state
type State = {
  didTimestamps     : Map[didBatchId: Hash, timestamp: Date]
  publishedDids     : Map[didSuffix: Hash, data: DIDDocument],
  credentialBatches : Map[credentialBatchId: Hash, data: CredentialBatch]
  revokedCredentials: Map[credentialBatchId: Hash, Map[credentialHash: Hash, credentialRevocationEvent: Date]]
}
```

Given the above, we define the node initial state as:

```scala
state = {
  didTimestamps      = Map.empty,
  publishedDids      = Map.empty,
  credentialBatches  = Map.empty,
  revokedCredentials = Map.empty
}
```

We will add an additional value called `BeginingOfTime` which will be used to represent timestamps for keys that belong
to unpublished DIDs.

## DID Creation

The DID creation process from v0.2 remains supported and unchanged.
It will run the same validations and only update the state of `publishedDids`.

## DID batch Timestamping

A difference with other operations is that to timestamp a batch of DIDs, we will not have
a signature involved. Recall that on DID creation we ask for a Master key signature.
So, given an operation:
 
```json
{
  "operation": {
    "timestampDIDBatch" : {
      "markleRoot" : ...
    }
  }
}
```
 
which posts a Merkle tree root hash, we update the state as follows:

```scala
state'.didTimestamps      = state.didTimestamps + { operation.timestampDIDBatch.markleRoot -> TX_TIMESTAMP }
state'.publishedDids      = state.publishedDids
state'.credentialBatches  = state.credentialBatches
state'.revokedCredentials = state.revokedCredentials
```

## DID Update

Given that we implemented on-chain batching, we do not need to update this operation. 
For completeness, we will describe the formal specification of this event.

```json
{
  "signedWith": KEY_ID,
  "signature": SIGNATURE,
  "operation": {
    "previousOperationHash": HASH,
    "updateDid": {
      "didSuffix": DID_SUFFIX,
      "actions": [
        ADD_KEY_ACTION | REMOVE_KEY_ACTION,
        ...
      ]
    }
  }
}
```

When the operation is observed in the stable part of the ledger, the node will act as before,
this is: 

  ```scala
    alias didToUpdate   = decoded.operation.updateDid.didSuffix
    alias signingKeyId  = decoded.signedWith
    alias updateActions = decoded.operation.updateDID.actions
    alias messageSigned = decoded.operation

    state.publishedDids.contains(didToUpdate) &&
    state.publishedDids(didToUpdate).keys.contains(signingKeyId) &&
    state.publishedDids(didToUpdate).keys(signingKeyId).usage == MasterKey &&
    state.publishedDids(didToUpdate).lastOperationReference == decoded.operation.previousOperationHash &&
    isValid(decoded.signature, messageSigned, state.publishedDids(didToUpdate).keys(signingKeyId).key) &&
    updateMap(signingKeyId, state.publishedDids(didToUpdate).keys, updateActions).nonEmpty 
  ```  

   where `updateMap` applies the updates sequentially over the initial state. It verifies that the operation signing key
   is not revoked by any action, that each action can be performed correctly and returns the updated `DidData` if 
   everything is fine. If any check or action application fails, an empty option is returned. 
   We will refine the specification of `updateMap` in a future iteration.

   If the check passes, then we get the following state update:
   ```scala
   state'.publishedDids = state.publishedDids.update(didToUpdate, { lastOperationReference = hash(decoded), 
                                                                    keys = updateMap(signingKeyId, state.publishedDids(didToUpdate).keys, updateActions).get }
   state'.didTimestamps = state.didTimestamps
   state'.credentialBatches  = state.credentialBatches
   state'.revokedCredentials = state.revokedCredentials
   ```

## Credential Batch Issuance

Now, similar to the situation with the first update operation, we find ourselves with an operation that will be signed
by a key referenced by a DID. We said that we implemented on-chain batching, so we do not need to embed the DID creation
event in this operation.

The old credential issuance operation can be described in the following way:

```json
{
  "keyId": KEY_ID,
  "signature": SIGNATURE,
  "operation": {
    "issueCredentialBatch": {
      "batchData": {
        "issuerDIDSuffix": DID_SUFFIX,
        "merkleRoot": MERKLE_TREE_ROOT
      }
    }
  }
}
```

The response is now a `batchId` that represents the batch analogous to the old `credentialId`:

```json
{
  "batchId" : OPERATION_HASH
}
```

Its implementation will be the hash of the `batchData` field.

The node state is now updated as follows:

    ```scala
    alias signingKeyId    = decoded.keyId
    alias issuerDIDSuffix = decoded.operation.issueCredential.batchData.issuerDIDSuffix 
    alias signature       = decoded.signature
    alias messageSigned   = decoded.operation

    state.dids.contains(issuerDIDSuffix) &&
    state.dids(issuerDIDSuffix).keys.contains(signingKeyId) &&
    state.dids(issuerDIDSuffix).keys(signingKeyId).usage == IssuingKey &&
    state.dids(issuerDIDSuffix).keys(signingKeyId).keyRevocationEvent.isEmpty &&
    isValid(signature, messageSigned, state.dids(issuerDIDSuffix).keys(signingKeyId).key)
    ```

   The state would be updated as follows:

   ```scala
    alias batchId = computeBatchId(decoded)
    alias merkleRoot = decoded.operation.issueCredential.batchData.merkleRoot 
    alias issuerDIDSuffix = decoded.operation.issueCredential.batchData.issuerDIDSuffix 

    state'.publishedDids = state.publishedDids
    state'.didTimestamps = state.didTimestamps
    state'.credentialBatches  = state.credentialBatches + { batchId -> {
                                                                         merkleRoot = merkleRoot, 
                                                                         issuerDIDSuffix = issuerDIDSuffix, 
                                                                         batchIssuingEvent    = LEDGER_TIMESTAMP,
                                                                         batchRevocationEvent = None
                                                                       }
                                                          }
    state'.revokedCredentials = state.revokedCredentials
   ```

## Batch Revocation

We now define the revocation for a batch of issued credentials.

```json
{
  "keyId": KEY_ID,
  "signature":: SIGNATURE,
  "operation": {
    "revokeBatch": {
      "batchId": HASH
    }
  }
}
```

The preconditions to apply the operation are:

```scala
alias batchId          = decoded.operation.revokeBatch.batchId
alias signature        = decoded.signature
alias signingKeyId     = decoded.keyId
alias messageSigned    = decoded.operation
alias issuerDIDSuffix  = state.credentialBatches(batchId).issuerDIDSuffix 

state.credentialBatches.contains(batchId) &&
state.credentialBatches(batchId).batchRevocationEvent.isEmpty && 
state.publishedDids.contains(issuerDIDSuffix) &&
state.publishedDids(issuerDIDSuffix).keys.contains(signingKeyId) &&
state.publishedDids(issuerDIDSuffix).keys(signingKeyId).usage == IssuingKey &&
state.publishedDids(issuerDIDSuffix).keys(signingKeyId).keyRevocationEvent.isEmpty && 
isValid(signature, messageSigned, state.publishedDids(issuerDIDSuffix).keys(signingKeyId).key)
```

If the precondition holds, we update the node state as follows:

```scala
alias batchId = decoded.operation.revokeBatch.batchId
alias initialBatchState = state.credentialBatches(batchId)

state'.publishedDids = state.publishedDids
state'.didTimestamps = state.didTimestamps
state'.revokedCredentials = state.revokedCredentials
state'.credentialBatches = state.credentialBatches + { batchId -> {
                                                                     issuerDIDSuffix = initialBatchState.issuerDIDSuffix, 
                                                                     merkleRoot = initialBatchState.merkleRoot,
                                                                     batchIssuingEvent = initialBatchState.batchIssuingEvent,
                                                                     batchRevocationEvent = Some(OPERATION_LEDGER_TIME)
                                                                  }
                                                     }
```

## Credentials Revocation

We are finally on the credential revocation operation. We will update the operation from version 0.2 to now allow many
credential hashes to revoke.

```json
{
  "keyId": KEY_ID,
  "signature":: SIGNATURE,
  "operation": {
    "revokeCredentials": {
      "batchId": HASH,
      "credentialHashes": [ HASH, ...],
    }
  }
}
```

The preconditions to apply the operation are:

```scala
alias batchId          = decoded.operation.revokeCredentials.batchId
alias signature        = decoded.signature
alias signingKeyId     = decoded.keyId
alias messageSigned    = decoded.operation
alias issuerDIDSuffix  = state.credentialBatches(batchId).issuerDIDSuffix 
alias credentialHashes = decoded.operation.revokeCredentials.credentialHashes

state.credentialBatches.contains(batchId) &&
// the batch was not already revoked
state.credentialBatches(batchId).batchRevocationEvent.isEmpty &&  
state.publishedDids.contains(issuerDIDSuffix) &&
state.publishedDids(issuerDIDSuffix).keys.contains(signingKeyId) &&
state.publishedDids(issuerDIDSuffix).keys(signingKeyId).usage == IssuingKey &&
state.publishedDids(issuerDIDSuffix).keys(signingKeyId).keyRevocationEvent.isEmpty && 
isValid(signature, messageSigned, state.publishedDids(issuerDIDSuffix).keys(signingKeyId).key)
```

If the above holds, then:

```scala
alias batchId          = decoded.operation.revokeCredential.batchId
// we will only update the state for the credentials not already revoked
alias credentialHashes = filterNotAlreadyRevoked(decoded.operation.revokeCredential.credentialHashes)
alias issuerDIDSuffix  = state.credentialBatches(batchId).issuerDIDSuffix 

state'.publishedDids = state.publishedDids
state'.didTimestamps = state.didTimestamps
state'.credentialBatches  = state.credentialBatches

state'.revokedCredentials = 
    state.revokedCredentials + { batchId -> state.revokedCredentials(batchId) + buildMap(credentialHashes)
                               } 
```

where `buildMap` takes the credential hashes and computes a map from the credential hashes to their revocation ledger 
time.

**Note:** The reader may realise that an issuer could mark as "revoked" credentials that are actually not part of the 
referred batch. This is because there is no check that the credential hashes shared are contained in the batch. The 
reason why we didn't add this check, is that merkle proof of inclusion for many credentials are big in size. 
We argue, however, that the verification process will take a credential and check for revocation _in its corresponding_ 
batch, meaning that no issuer will be able to revoke other issuers credentials (or even a credential outside of its 
issuance batch). 

## Credential Verification 

In order to describe credential verification, we assume the following data to be present in the credential.

```json
{
  "issuerDID" : DiD,
  "signature" : SIGNATURE,
  "keyID" : KEY_ID,
  ...
}
```

We also assume the presence of a Merkle proof of inclusion, `merkleProof` that attests that the credential is contained
in its corresponding batch.

Given a credential `c` and its Merkle proof of inclusion `mproof`, we define `c` to be valid if and only if the 
following holds:

```scala
alias computedMerkleRoot = computeRoot(c, mproof)
alias batchId = computeBatchId(c, computedMerkleRoot) // we combine the data to compute the batch id
alias issuerDIDSuffix = c.issuerDID.suffix
alias keyId = c.keyId
alias signature = c.signature
alias credentialHash = hash(c)

// control key data
state.publishedDids.contains(issuerDIDSuffix) &&
state.publishedDids(issuerDIDSuffix).keys.contains(keyId) &&
state.publishedDids(issuerDIDSuffix).keys(keyId).usage == IssuingKey &&
// check that the credential batch matches the credential data
state.credentialBatches.contains(batchId) &&
state.credentialBatches.contains(batchId).issuerDIDSuffix == issuerDIDSuffix &&
state.credentialBatches.contains(batchId).merkleRoot == computedMerkleRoot &&
// check that the batch was not revoked
state.credentialBatches(batchId).batchRevocationEvent.isEmpty &&
// check that the specific credential was not revoked
( 
  ! state.revokedCredentials.contains(batchId) ||
  ! state.revokedCredentials(batchId).contains(credentialHash)
) &&
// check the key timestamp compared to the credential issuance timestamp
(
  state.publishedDids(issuerDIDSuffix).keys(keyId).keyAdditiontionEvent < state.credentialBatches(batchId).batchIssuingEvent &&
  (
    state.publishedDids(issuerDIDSuffix).keys(keyId).keyRevocationEvent.isEmpty ||
    state.credentialBatches(batchId).batchIssuingEvent < state.publishedDids(issuerDIDSuffix).keys(keyId).keyRevocationEvent.get
  ) 
) &&
// check the credential signature
isValid(signature, c, state.publishedDids(issuerDIDSuffix).keys(keyId).key)
```

We want to remark that the key that signs a credential does not need to be the same key that signs the credential batch
issuance event. This is why we add checks to control that the signing key in the credential was already present in the
DID at the time the batch occurred and, if revoked, we request that the revocation occurred after the credential was
issued.

We also want to remark that, currently, the key that signs the credential and the key that signs the issuance operation 
must belong to the same DID. The reason is that the DID extracted from the credential is the one used to compute the 
`batchId`. In order to allow one DID to sign the credential and another one to sign the issuance operation, we would 
need to add two DIDs to the credential and specify which DID should be used for each check.
