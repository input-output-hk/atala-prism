<!-- This is meant to be part of a larger document -->

\newpage

# Slayer v2: Cardano 2nd layer protocol

This document describes the protocol for building the credentials project on top of Cardano.
The main differences with version v0.1 of the protocol are:

- We now post Atala operations one at a time instead of posting them in batches
- DID Documents' underlying data is posted along with DIDs on the blockchain during the creation event

Those decisions allow this protocol to work in the open setting while avoiding attacks 
described in version 0.1. Another observation is that the content addressable storage 
now has no need to store public data (DID Documents).

## Definitions

- **DID**: A decentralized identifier, see the [official spec](https://w3c-ccg.github.io/did-spec/).
- **DID Document**: The document representing a state of a DID, includes details like its public keys
- **Verifiable Credential**: A signed digital document. E.g. digital university degrees, digital passport, etc.
- **Atala Node**: An application that follows this protocol. It sends transactions with metadata to the Cardano 
  blockchain, read transactions confirmed by Cardano nodes, and interprets the relevant information from metadata
  in compliance with protocol rules 
- **Atala Operation**: Metadata attached to a Cardano transaction, it is used to codify protocol events like 
  `Create DID`, `Issue Credential`, etc, look into the [supported operations](#Operations) for more details
- **Atala Transaction**: A Cardano transaction with an Atala Operation in its metadata
- **Genesis Atala Block**: The first Cardano Block that contains an Atala transaction, everything before this block can 
  be discarded by Atala Nodes

Throughout this document, we will refer to DIDs, DID Documents and verifiable credentials as _entities_.  
We will also user the term _Atala Event_ to refer to the event of detecting a new Atala operation in the stable part of 
Cardano's blockchain. In particular, we will refer to events named after the operations e.g. DID creation event, 
credential revocation event, etc. 
Finally, we will say that a node _processes_ an operation when it reads it from the blockchain and that it _posts_ an
operation when a client sends a request to it to publish it on the blockchain.

## Protocol

### Objective

The goal of the protocol is to read Atala operations from the stable section of Cardano's blockchain, and keep an updated
state of DIDs, DID Documents and verifiable credentials. The protocol operations modify the state of the said entities.
The current list of operations is:
- **Create DID**: Allows to create and publish a new DID and its corresponding DID Document
- **Update DID**: Allows to either add or revoke public keys to/from a DID Document
- **Issue Credential**: Allows an issuer to publish a proof of issuance in Cardano's blockchain
- **Revoke Credential**: Allows to revoke an already published verifiable credential 

Along the operations, the protocol also provides services that any node must implement that do not affect the state.
The list of services is:
- **(DID resolution)** Given a published DID, return its corresponding DID Document in its latest state
- **(Pre validation)** Given a credential's hash, a signer's DID, a key identifier, and an expiration date, reply if the 
  credential is invalid with respect to signing dates and keys correspondence. The operation returns a key if the 
  validations pass. The client needs to validate cryptographic signatures with the provided key. See 
  [credential validation](#Verification) section for more details.

Possible future operations
- **Revoke DID**: Allows to mark a DID as invalid. Rendering all its keys invalid.

### Operation posting and validation
 
Atala operations are encoded messages that represent state transitions. The messages' structure and protocol state will 
be described later in this document. When a node posts an operation, a Cardano transaction is created and submitted to 
the Cardano network. The transaction includes the encoded operation on its metadata. In Bitcoin we used to prepend the 
metadata messages with the string `FFFF0000`, a magic value (to be defined) which told Atala Nodes that this transaction
has an Atala operation linked in it. We will consider doing the same in Cardano blockchain once we get more details on 
the metadata field structure. 

After the published Cardano transaction has N (to be defined) confirmations, we could consider the transaction as final 
(no rollback expected), which could mark the underlying operation as final.
 
There are two definitions related to operations:
 - We say that an operation is **valid** if it is well-formed - has all required fields - and it is not larger than 
   defined limits. Validity depends only on the operation itself and system parameters, not on its state. 
 - We say that operation is **correct** if it is valid, signed properly, and it is either creation operation or refers 
   the previous operation that affects the same entity.
    
Operations that are not valid must be discarded by the node and not included in a transaction. On the other hand 
operations correctness should not be checked before inclusion. It is checked during state updates - incorrect operations 
are ignored.
    
### Node state
  
In order to describe the protocol, we will consider the following state that each node will represent. We will define 
the protocol operations and services in terms of how they affect/interact with this abstract state. 

```scala
// Abstract types
type Key  
type Hash
type Date

Concrete types
type KeyUsage       = MasterKey | IssuingKey | AuthenticationKey | CommunicationKey
type KeyData        = {
  key: Key, 
  usage: KeyUsage, 
  keyAdditiontionEvent: Date,      // extracted from the blockchain
  keyRevocationEvent: Option[Date] // extracted from the blockchain
}
type DIDData        = {
  lastOperationReference: Hash, 
  keys: Map[keyId: String, keyData: KeyData]
}
type CredentialData = {
  lastOperationReference: Hash, 
  issuerDIDSuffix: Hash, 
  credIssuingEvent: Date,            // extracted from the blockchain
  credRevocationEvent: Option[Date]  // extracted from the blockchain
}

// Node state
state = {
  dids       : Map[didSuffix: Hash, data: DIDDocument],
  credentials: Map[credentialId: Hash, data: CredentialData]
}
```

We want to remark that the fields
- `keyAdditionEvent`
- `keyRevocationEvent`
- `credIssuingEvent`
- `credRevocationEvent`
represent **timestamps inferred from Cardano's blockchain and not data provided by users**

The representation is implementation independent and applications can decide to optimise it with databases, 
specialized data structures, or others. We aim to be abstract enough for any implementation to be able to map itself 
to the abstract description.

Let's now move into the operations descriptions.

### Operations

Users send operations via RPC to nodes. Each operation request needs to be signed by the relevant key (specified for 
each operation) and wrapped into `SignedAtalaOperation` message. Signature is generated from byte sequence obtained by 
binary encoding of `AtalaOperation` message.

Operations must contain the exact fields defined by schemas, unless specified otherwise. If there is an extra or missing 
value, the operation is considered to be invalid.

We can divide operations into two kinds: ones that create a new entity (e.g. CreateDID or IssueCredential) and ones 
that affect existing one (e.g. UpdateDID, RevokeCredential). The latter always contain a field with previous operation 
hash (SHA256 of `AtalaOperation` binary encoding). If the hash doesn't match with the last performed operation on the 
entity, the operation is considered incorrect and it is ignored.

When describing checks or effects of an operation on the node state, we will use an implicit value `decoded` that 
represents the decoded operation extracted from the blockchain.

#### Create DID

Registers DID into the ledger. The associated initial DID Document structure here is very simple: it consists only of
a document id (the DID) and sequence of public keys with their respective key ids. It must be signed by one of the
master keys given in the DID Document.

```json
{
  "signedWith": "master",
  "signature": "MEQCIBZGvHHcSY7AVsds/HqfwPCiIqxHlsi1m59hsUWeNkh3AiAWvvAUeF8jFgKLyTt11RNOQmbR3SIPXJJUhyI6yL90tA==",
  "operation": {
    "createDid": {
      "didData": {
        "publicKeys": [
          {
            "id": "master",
            "usage": "MASTER_KEY",
            "ecKeyData": {
              "curve": "P-256K",
              "x": "8GnNreb3fFyYYO+DdiYd2O9SKXXGHvy6Wt3z4IuRDTM=",
              "y": "04uwqhI3JbY7W3+v+y3S8E2ydKSj9NXV0uS61Mem0y0="
            }
          },
          {
            "id": "issuing",
            "usage": "ISSUING_KEY",
            "ecKeyData": {
              "curve": "P-256K",
              "x": "F8lkVEMP4pyXa+U/nE2Qp9iA/Z82Tq6WD2beuaMK2m4=",
              "y": "2hHElksDscwWYXZCx1pRyj9XaOHioYr48FPNRsUBAqY="
            }
          }
        ]
      }
    }
  }
}
```

RPC Response:

```json
{
  "id": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39"
}
```

The returned identifier is hex encoding of the hash of the binary representation of `AtalaOperation` message. The DID 
can be obtained by prefixing the id with "did:atala:".

When the operation is observed in the stable part of the ledger, the node performs the following checks:

```scala
alias didSuffix = getDIDSuffix(decoded)
alias referredKey: Option[Key] = extractKey(decoded) 
alias messageSigned   = decoded.operation

 referredKey.nonEmpty &&
 referredKey.get.usage == MasterKey &&
 isValid(decoded.signature, messageSigned, referredKey.get) &&
 ! state.dids.contains(didSuffix)
```

where `extractKey` searches the key mentioned in `signedWith` from the decoded operation and return the key if found or
an empty option otherwise. 

If the check passes, then we get the following state update:

```scala
state'.dids = state.dids + (didSuffix -> { lastOperationReference = hash(decoded), 
                                           keys = createMap(decoded.operation.didData.publicKeys) } )
state'.credentials = state.credentials
```

where `createMap` maps the data from the request to the model

### Update DID

Updates DID content by sequentially running update actions included. Actions available: **AddKeyAction** and 
**RevokeKeyAction**. Please note that:
- The key id must be unique - when a key is revoked its id cannot be reused.
- A revoked key can't be re-added.

The operation must be signed by a master key. Actions cannot include revocation of the key used to sign the operation. 
In such case the operation is considered invalid. This is to protect against losing control of the DID. We assure that 
there is always one master key present that the user is able to sign data with. 
In order to replace the last master key, the user first has to add a new master key and then revoke the previous one 
in a separate operation signed by the newly added key. 
In order for the operation to be considered valid, all its actions need to be.

```json
{
  "signedWith": "master",
  "signature": "MEQCIGtIUUVSsuRlRWwN6zMzaSi7FImvRRbjId7Fu/akOxFeAiAavOigmiJ5qQ2ORknhAEb207/2aNkQKfzBr0Vw+JS+lw==",
  "operation": {
    "previousOperationHash": "o8rLLZ5RvdQCZLKH2xW0Eh3e6E6vuMPaVFyIwdmblNQ=",
    "updateDid": {
      "didSuffix": "o5fHLw4RvdQCZLKH2xW0Eh3e6E6vuMPaVeGDIwdmblaD=",
      "actions": [
        {
          "addKey": {
            "key": {
              "id": "issuing-new",
              "usage": "ISSUING_KEY",
              "ecKeyData": {
                "curve": "P-256K",
                "x": "Zk85VxZ1VTo2dxMeI9SCuqcNYHvW7mfyIPR0D9PI9Ic=",
                "y": "QsI8QhEe4Z0YnG4kGZglvYfEPME5mjxmWIaaxsivz5g="
              }
            }
          }
        },
        {
          "revokeKey": {
            "keyId": "issuing"
          }
        }
      ]
    }
  }
}
```

Response:

```json
{}
```

When the operation is observed in the stable part of the ledger, the node performs the following checks:

```scala
alias didToUpdate = decoded.operation.UpdateDID.didSuffix
alias signingKeyId = decoded.signedWith
alias updateActions = decoded.operation.updateDID.actions
alias currentDidData = state.dids(didToUpdate).keys
alias messageSigned   = decoded.operation

state.dids.contains(didToUpdate) &&
state.dids(didToUpdate).keys.contains(signingKeyId) &&
state.dids(didToUpdate).keys(signingKeyId).usage == MasterKey &&
state.dids(didToUpdate).lastOperationReference == decoded.operation.previousOperationHash &&
isValid(decoded.signature, messageSigned, state.dids(didToUpdate).keys(signingKeyId).key) &&
updateMap(signingKeyId, currentDidData, updateActions).nonEmpty

```

where `updateMap` applies the updates sequentially over the initial state. It verifies that the operation signing key is
not revoked by any action, that each action can be performed correctly and returns the updated DidData if everything is
fine. If any check or action application fails, an empty option is returned. 
We will refine the specification of `updateMap` in a future iteration.


If the check passes, then we get the following state update:

```scala
state'.dids = state.dids.update(didToUpdate, { lastOperationReference = hash(decoded), 
                                               keys = updateMap(signingKeyId, currentDidData, updateActions).get }
state'.credentials = state.credentials
```

### IssueCredential

Publishes a proof of existence for a credential given hash of its contents. It must be signed by one of issuer's current 
issuing keys.

**NOTES**:
- We are not enforcing that the signature of the RPC is the same as the credential's signature. Should we add this? It 
  could be useful to keep flexibility and allow signing with different keys. For example, a university may have a DID 
  and be the authority to issue credentials while the credential signers could be other institutions under the university
  control. The revocation control lives still under the sole control of the university. Note that depending on this decision
  we need to add or remove checks in the verification process. I am currently assuming that the keys could be different. 

```json
{
  "keyId": "issuing",
  "signature": "MEUCIQDCntn4GKNBja9LYPHa5U7KSQPukQYwHD2FuxXmC2I2QQIgEdN3EtFZW+k/zOe2KQYjYZWPaV5SE0Mnn8XmhDu1vg4=",
  "operation": {
    "issueCredential": {
      "credentialData": {
        "issuerDIDSuffix": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39",
        "contentHash": "7XACtDnprIRfIjV9giusFERzD722AW0+yUMil7nsn3M="
      }
    }
  }
}
```

RPC response:

```json
{
  "id": "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
}
```

The returned identifier is hex encoding of the hash of the binary representation of `AtalaOperation` message. 
It is used to refer the credential in the revocation operation.

When the operation is observed in the stable part of the ledger, the node performs the following checks:

```
alias signingKeyId    = decoded.keyId
alias issuerDIDSuffix = decoded.operation.issueCredential.credentialData.issuerDIDSuffix 
alias signature       = decoded.signature
alias messageSigned   = decoded.operation

state.dids.contains(issuerDIDSuffix) &&
state.dids(issuerDIDSuffix).keys.contains(signingKeyId) &&
state.dids(issuerDIDSuffix).keys(signingKeyId).usage == IssuingKey &&
state.dids(issuerDIDSuffix).keys(signingKeyId).keyRevocationEvent.isEmpty &&
isValid(signature, messageSigned, state.dids(issuerDIDSuffix).keys(signingKeyId).key)
```

If the check passes, then we get the following state update:

```scala
alias credentialId = getCredId(decoded)

state'.dids = state.dids
state'.credentials = { state.credentials + (credentialId -> { lastOperationReference = hash(decoded), 
                                                              issuerDIDSuffix        = issuerDIDSuffix, 
                                                              credIssuingEvent       = BLOCK_TIMESTAMP
                                                              credRevocationEvent    = None
                                                             })
}
```

#### RevokeCredential

It must be signed by one of issuer's current issuing keys.

Example:

```json
{
  "keyId": "issuing",
  "signature": "MEUCIQCbX9aHbFGeeexwT7IOA/n93XZblxFMaJrBpsXK99I3NwIgQgkrkXPr6ExyflwPMIH4Yb3skqBhhz0LOLFrTqtev44=",
  "operation": {
    "revokeCredential": {
      "previousOperationHash": "o8rLLZ5RvdQCZLKH2xW0Eh3e6E6vuMPaVFyIwdmblNQ=",
      "credentialId": "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
    }
  }
}
```

When the operation is observed in the stable part of the ledger, the node performs the following checks:

```
alias signingKeyId    = decoded.keyId
alias credentialId    = decoded.operation.revokeCredential.credentialId
alias issuerDIDSuffix = state.credentials(credentialId).issuerDIDSuffix 
alias signature       = decoded.signature
alias messageSigned   = decoded.operation
alias previousHash    = decoded.operation.revokeCredential.previousOperationHash

state.credentials.contains(credentialId) &&
state.credentials(credentialId).lastOperationReference == previousHash &&
state.dids.contains(issuerDIDSuffix) &&
state.dids(issuerDIDSuffix).keys.contains(signingKeyId) &&
state.dids(issuerDIDSuffix).keys(signingKeyId).usage == IssuingKey &&
state.dids(issuerDIDSuffix).keys(signingKeyId).keyRevocationEvent.isEmpty && 
isValid(signature, messageSigned, state.dids(issuerDIDSuffix).keys(signingKeyId).key)
```

If the check passes, then we get the following state update:

```scala
state'.dids = state.dids
state'.credentials = { state.credentials + (credentialId -> { lastOperationReference = hash(decoded), 
                                                              issuerDIDSuffix        = state.credentials(credentialId).issuerDIDSuffix, 
                                                              credIssuingEvent       = state.credentials(credentialId).credIssuingEvent,,
                                                              credRevocationEvent    = Some(BLOCK_TIMESTAMP)
                                                             })
}
```

RPC response:

```scala
{}
```

### Services

#### Verification

In order to define credential verification we need to define what do we mean by a credential being valid.
Here we have to distinguish between two types of validity. Given a credential, one notion of validity relates to the 
credential's specific semantic. E.g. a credential could represent a contract between two parties. In such case, our 
protocol has no say on telling if the contract signed is valid with respect to the legal system applied in a certain 
jurisdiction. However, the protocol probably should guarantee that the contract was published on a certain date, was 
signed by an specific issuer with a specific valid key, its expiration date (if any) hasn't occur, and guarantee that 
the credential's content was not altered.

We could define the former type of validity as _credential specific validity_ and the later as _protocol validity_. 
Note that some credentials may not have any specific validations outside the protocol ones. E.g. birth certificates,
university degrees, national id documents, may only require the protocol validity. But other credentials may require
additional ones on top of those.

For credential specific validations, we think that specific applications should be built on top of our protocol 
to fulfil specific use cases. The process described below will formalise the steps to verify protocol validity. Note 
that **all** credentials need to be valid according to the protocol independently to the existence of other credential
specific validations. From now on, we will use the term valid referring to protocol valid. 

Intuitively speaking, a credential is valid when an issuer signs it with a valid key and posts the Atala operation in 
the blockchain. The credential will remain valid as long as the credential's expiration date (if any) hasn't passed,
and there must be no revocation operation posted in the blockchain referring to that credential. Otherwise the 
credential is considered invalid.
The credential needs to have the following information to perform this checks:

```scala
- expirationDate: Option[Date]
- issuerDID: DID
- signature: Bytes
- signingKey: String // reference to a key in the issuers DID Document
```

The key is considered valid if it was added in the `issuerDID`'s DID Document before `credIssuingEvent` and, if the key 
is revoked in the said DID Document, the revocation event occurred after `credIssuingEvent` (i.e. after the credential
was recorded as issued). 
As mentioned during the description of Issue Credential event, if we enforce that the key that signs the issuance 
request must be the same as the one that signs the credential, then we don't need to check that the credential was 
signed in proper dates because this will be guaranteed by the check performed in the IssueCredential operation. 
If we want to keep the flexibility of using different keys, we need to validate the relation between dates of key 
additions/revocations and credential issuance. 
The flexibility could be useful for an institution that allows other sub-institutions to sign credentials but only 
the main institution can publish the credentials to the blockchain. This same institution is the one used for 
revocation. An example of such relation could be faculties signing university degrees but only the university
main administration is the one with the power to issue/revoke a credential. We should note that this could be
simulated with a single DID by having different issuing keys for the faculties.

Expressed with respect to the node state, given a credential C:

```scala
// C has no expiration date or the expiration date hasn't occur yet, and
(C.expirationDate.isEmpty || C.expirationDate.get >= TODAY) && 
// the credential was posted in the chain, and
state.credentials.get(hash(C)).nonEmpty &&
// the credential was not revoked, and
state.credentials(hash(C)).data.credRevocationEvent.isEmpty &&
// the issuer DID that signed the credential is registered, and
state.dids.get(C.issuerDID.suffix).nonEmpty &&
// the key used to signed the credential is in the DID, and
state.dids(C.issuerDID.suffix).data.get(C.signingKey).nonEmpty
// the key was in the DID before the credential publication event, and
state.dids(C.issuerDID.suffix).data(C.signingKey).keyPublicationEvent < state.credentials(hash(C)).data.credPublicationEvent &&
// the key was not revoked before credential publication event, and
(
  // either the key was never revoked
  state.dids(C.issuerDID.suffix).data(C.signingKey).keyRevocationEvent.isEmpty ||
  // or was revoked after signing the credential
  state.credentials(hash(C)).data.credPublicationEvent < state.dids(C.issuerDID.suffix).data(C.signingKey).keyRevocationEvent.get
)
// the signature is valid
isValidSignature(
   C, 
   state.dids(C.issuerDID.suffix).data(C.signingKey).key
)
```

## Tentative schemas

### Verifiable Credential

After reviewing the verification process we can propose this tentative generic credential schema.
The schema could move around the fields, e.g. the signature field could be an object that contains the key reference.

```scala
{
  credentialName: String
  expirationDate: Option[Date]
  issuerDID: DID
  signature: Bytes
  signingKey: String // reference to a key in the issuers DID Document
  claim : Object // contains a mapping of key -> values that represent the credential specific data 
}
```

We should note that this schema is not compliant with W3C standard drafts. It could be adapted
on many parts.
 
However, the standard proposes a `credentialStatus` [field](https://www.w3.org/TR/vc-data-model/#status) for which
documentation currently states:

```text
credentialStatus
  The value of the credentialStatus property MUST include the:
  + id property, which MUST be a URL.
  + type property, which expresses the credential status type (also referred to as the credential status method). It is 
    expected that the value will provide enough information to determine the current status of the credential. For 
    example, the object could contain a link to an external document noting whether or not the credential is suspended 
    or revoked.
  The precise contents of the credential status information is determined by the specific credentialStatus type 
  definition, and varies depending on factors such as whether it is simple to implement or if it is privacy-enhancing.
```

and later says

```text
Defining the data model, formats, and protocols for status schemes are out of scope for this specification. A Verifiable 
Credential Extension Registry [VC-EXTENSION-REGISTRY] exists that contains available status schemes for implementers who 
want to implement verifiable credential status checking.
```

but the [extension registry](https://w3c-ccg.github.io/vc-extension-registry/) provides no relevant data.

It is unclear at the moment of this writing how to define this field. We may need to
review more sections in the standard to clarify this point.

## Notes

Below we raise points that may affect the protocol and schemas. We also note tasks
related to the protocol that we should perform.

- Add a `RecoveryKey` value to the `KeyUsage` type. It could be useful to allow recovering the control over a DID even
  if the master key is lost/compromised. It should be non-revocable by any other key but itself (leaving an exception to
  our DIDUpdate operation).
- Allow to publish both batched operations or individual operations in transactions' metadata. 
  Batching operations forces us the need to maintain a CAS which also brings possible problems related to files missing 
  in a CAS. On the other hand, batching operations reduces fees. If IOHK restricts who can issue operations (in order 
  to ease problems related to who add files into the CAS), then users could complain about centralization. We find then,
  as a reasonable approach, to allow any user to post an operation as long as it entire content is stored directly in 
  the transaction metadata. This allows for a batching service provided by trusted parties and also the option to 
  maintain independence for those who prefer it.
- Allow issuers to not publish an issuance operation in the blockchain. During conversations we noted that the protocol 
  always publishes a proof of existence on-chain along with the issuer DID. This allows for any actor to count how many
  credentials an issuer produces. This may be useful for some cases and we wonder if it could be a problem for others. 
  If we remove the correlation of publication events with issuer's data, then the issuer looses the ability to detect if 
  a credential is issued without authorization (e.g. due to a compromised key).
  If we want this correlation to be optional, we need to change the verification process and also define how issuers 
  would specify which type of credentials they issue. Note that issuers need to specify somewhere that their credentials
  require a publication event to be valid, if not an attacker could simple issue credentials without this event and nodes
  wouldn't detect that the event is required. A possible place for such configuration is the issuer's DID itself, but we
  should analyse the limitations of such approach.
- Define an approach to privacy and share partial information from credentials.
  This is needed for compliance with W3C. This could also enable many interesting use cases. 
- Define if we intend to be W3C standard, if so, update schemas and operations appropriately.
- Estimate bytes consumed by normal operation and estimate ADA fees.
- Define processes to establish trust on DIDs and how to map real world identities behind them.
- Define if we need credentials with multiple issuers.
- The W3C standard mentions `validFrom` and `validUntil` fields (where `validFrom` can be a date in the future). Decide 
  if we want to update the verification process based on such fields and how to manage date verification (e.g. should we 
  simply trust the issuer?).
- We can add other credential statuses. This is, instead of publishing a credential revocation event, we could post an 
  update event that adds other statuses like `temporaly suspended` which   can then transition back to `issued`. We 
  should consider privacy implications of such update.
  E.g. a driver licence could keep forever a trace of times it has been suspended.
- We should define a way to inform verifiers more information about the credential status. This is akin to what the
  `credentialStatus` field attempts to do, which is basically to have a URL to check status and other information (like
  revocation reason). We should review if this field is mandatory or optional in the standard.
- If we decide to batch operations in files, we should consider how to order them. We could list first all DID related
  operations and then the credentials related ones. The motivation is that a user could send to a node multiple requests 
  and expect the order to be preserved, e.g. first create a DID and then issue a sequence of credentials. If the order 
  of events is inverted by the node, then we may end up rejecting the issuance of credentials and then creating a DID. 
  An alternative to order events could be to provide a different endpoint for the node to receive batches of operations.
