<!-- This is meant to be part of a larger document -->

\newpage

# Slayer v1: Bitcoin 2nd layer protocol

This document describes the protocol for building the credentials project as a 2nd layer on top of Bitcoin. The protocol is based on the [sidetree protocol](https://github.com/decentralized-identity/sidetree/blob/master/docs/protocol.md) from Microsoft.

## Definitions

The definitions from the [high-level-design-document](https://github.com/input-output-hk/atala/tree/develop/prism-backend/docs/bitcoin#definitions) apply here, be sure to review those ones first.

- **DID**: A decentralized identifier, see the [official spec](https://w3c-ccg.github.io/did-spec/).
- **DID Document**: The document representing a state of a DID, includes details like its public keys.
- **ATALA Operation**: An operation in the 2nd layer ledger (ATALA Node), like `Create DID`, `Issue Credential`, etc, look into the [supported operations](#atala-operations) for more details. This is equivalent to a Bitcoin Transaction on the 2nd layer protocol.
- **ATALA Block**: A list of ATALA Operations, includes metadata about the block. This is equivalent to a Bitcoin Block on the 2nd layer protocol.
- **ATALA Object**: A file that has metadata about an ATALA Block and a reference to retrieve it. A reference to an ATALA Object is the only detail stored in Bitcoin operations.
- **Genesis Bitcoin Block**: The first Bitcoin Block aware of ATALA Objects, everything before this block can be discarded.
- **Content Addressable Storage**: System that allows storing files and querying them based on the hash of the file. We have yet to choose whether to use centralized one (e.g. S3), decentralized (IPFS) or some combination of these.

## Cryptography

We use **hashing** to create short digests of potentially long data. For secure cryptographic hash algorithms there is an assumption that it is computationally intractable to find two values that have the same hash. This is why the hash is enough to uniquely identify the value. SHA256 algorithm is used, unless specified otherwise.

Cryptographic signatures are used to assure person who generated the message is who they claim they are. Keys are generated and managed by users - you can read more on that in [Key management](#key-management) section. SHA256 with ECDSA is used unless specified otherwise.


## Differences from sidetree

This section states the main known differences from the sidetree protocol:

- We support credentials while sidetree only supports DIDs.

- We are using Protobuf encoded data while sidetree uses JSON representation.

- We are using GRPC for API while sidetree uses REST.


## The path to publish an ATALA Operation
There are some steps involved before publishing an ATALA Operation, the following steps show what the ATALA Node does to publish a credential proof, publishing any other ATALA Operation would follow the same process.

**NOTE**: We are using JSON protobuf encoding for readability here. Binary encoding is used for querying the node and storage.

1- Let's assume that issuer's DID is already registered on the ledger. In order to register an issued credential they need to send a signed ATALA operation containing its hash:

```json
IssueCredentialOperation:
{
  "signedWith": "issuing",
  "signature": "MEUCIQDCntn4GKNBja9LYPHa5U7KSQPukQYwHD2FuxXmC2I2QQIgEdN3EtFZW+k/zOe2KQYjYZWPaV5SE0Mnn8XmhDu1vg4=",
  "operation": {
    "issueCredential": {
      "credentialData": {
        "issuer": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39",
        "contentHash": "7XACtDnprIRfIjV9giusFERzD722AW0+yUMil7nsn3M="
      }
    }
  }
}
```

2- After some time the node creates an ATALA Block containing all operations it has received from clients. Here it contains just one, but generally nodes will batch operations in order to lower block publishing costs.

```json
{
  "version": "0.1",
  "operations": [
    {
      "signedWith": "issuing",
      "signature": "MEUCIQDCntn4GKNBja9LYPHa5U7KSQPukQYwHD2FuxXmC2I2QQIgEdN3EtFZW+k/zOe2KQYjYZWPaV5SE0Mnn8XmhDu1vg4=",
      "operation": {
        "issueCredential": {
          "credentialData": {
            "issuer": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39",
            "contentHash": "7XACtDnprIRfIjV9giusFERzD722AW0+yUMil7nsn3M="
          }
        }
      }
    }
  ]
}
```

3- An ATALA Object is created, it links the current ATALA Block. In the future we plan to add metadata which can help the ATALA Node to choose whether to retrieve the ATALA Block or not (think about a light client), such as list of ids of entities that the block affects. ATALA Block hash included is SHA256 hash of block file content - which consists of binary encoded `AtalaBlock` protobuf message.

```json
{
  "blockHash": "s4Xy4+Cx4b1KaH1CHq4/kq9yzre3Uwk2A0SZmD1t7YQ=",
  "blockOperationCount": 1,
  "blockByteLength": 196
}
```


4- The ATALA Block and the ATALA Object are pushed to the Content Addressable Storage, each of these files are identified by the hash of its content, like `HASH(ATALA Object)` and `HASH(ATALA Block)` - the hash is computed from the file contents, binary encoded protobuf message.

5- A Bitcoin transaction is created and submitted to the Bitcoin network, it includes a reference to the ATALA Object on the special output (see the one with OP_RETURN), the `FFFF0000` is a magic value (to be defined) which tells ATALA Nodes that this transaction has an ATALA Object linked in it, after the magic value, you can see the `HASH(ATALA Object)` (SHA256 of the file).

```json
{
  "txid": "0b6e7f92b27c70a6948dd144fe90387397c35a478f8b253ed9feef692677185e",
  "vin": [...],
  "vout": [
    {
      "value": 0,
      "n": 0,
      "scriptPubKey": {
        "asm": "OP_RETURN FFFF0000 015f510a36c137884c6f4527380a3fc57a32fdda79bfd18634ba9f793edb79c2",
        "hex": "6a2258...",
        "type": "nulldata"
      }
    }
  ],
  "hex": "01000000...",
  "time": 1526723817
}
```

6- After the published Bitcoin transaction has N (to be defined) confirmations, we could consider the transaction as final (no rollback expected), which could mark all the underlying ATALA operations as final.

## Validation

There are two notions related to operations: we say that an operation is **valid** if it is well-formed - has all required fields - and it is not larger than defined limits. Validity depends only on the operation itself and system parameters, not on its state. We say that operation is **correct** if it is valid, signed properly, and it is either creation operation or refers the previous operation correctly.

Operations that are not valid must be discarded by the node and not included in the block. Inclusion of even one invalid operation renders the whole block invalid and it must be ignored by the nodes. On the other hand operations correctness should not be checked before inclusion - the block can include incorrect operations. It is checked during state updates - incorrect operations are ignored, but one or more incorrect operations don't affect processing other operations in the block.


## Updating ATALA Node state
The ATALA Node has an internal database where it indexes the content from the ATALA operations, this is useful to be able to query details efficiently, like retrieving a specific DID Document, or whether a credential was issued.

The ATALA Node needs to have a Genesis Bitcoin Block specified, everything before that block doesn't affect the ATALA Node State.

These are some details that the ATALA Node state holds:

- DIDs and their respective current document.
- Credentials (hashes only), who issued them and when they were revoked (if applies)


The component updating the ATALA Node state is called the Synchronizer.

The Synchronizer keeps listening for new blocks on the Bitcoin node, stores the Bitcoin block headers and applies rollbacks when necessary.

After block is considered finalized (getting N confirmations), we must look for ATALA Objects, retrieve the object from the storage, perform the validations (to be defined) on the ATALA Object, the ATALA Block, and the ATALA Operations, and apply the ATALA Operations to the current ATALA Node state. That means that the state is lagging N blocks behind what is present in the Bitcoin ledger - and all user queries are replied to basing on such lagging state.


## ATALA Operations

Here you can find the possible ATALA Operations. The list will be updated when new operation get implemented.

Each operation sent via RPC to node needs to be signed by the client using relevant key (specified in the operation description) and wrapped into `SignedAtalaOperation` message. Signature is generated from byte sequence obtained by binary encoding of `AtalaOperation` message.

Operations must contain all fields, unless specified otherwise. If there is a value missing, the operation is considered invalid.

We can divide operations into two kinds: ones that create a new entity (e.g. CreateDID or IssueCredential) and ones that affect existing one (e.g. RevokeCredential). The latter always contain a field with previous operation hash (SHA256 of `AtalaOperation` binary encoding). If it doesn't match, operation is considered incorrect and it is ignored.

### CreateDID

Registers DID into the ATALA ledger. DID structure here is very simple: it consists only of id and sequence of public keys with their ids. It must be signed by one of the master keys given in the document.

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

The returned identifier is hex encoding of the hash of the binary representation of `AtalaOperation` message. The DID can be obtained by prefixing the id with "did:atala:".

### UpdateDIDOperation

Updates DID content by sequentially running update actions included. Actions available: **AddKeyAction** and **RemoveKeyAction**. Please note that key id must be unique - when a key is removed its id cannot be reused.

The operation must be signed by a master key. Actions cannot include removal of the key used to sign the operation - in such case the operation is considered invalid. That is to protect agains losing control of the DID - we assure that there is always one master key present that the user is able to sign data with. In order to replace the master key, the user has to issue first operation adding new master key and then another removing previous one. In order for the operation to be considered valid, all its actions need to be.

```json
{
  "signedWith": "master",
  "signature": "MEQCIGtIUUVSsuRlRWwN6zMzaSi7FImvRRbjId7Fu/akOxFeAiAavOigmiJ5qQ2ORknhAEb207/2aNkQKfzBr0Vw+JS+lw==",
  "operation": {
    "updateDid": {
      "id": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39",
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
          "removeKey": {
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

### IssueCredential

Register credential into the ATALA ledger, given hash of its contents. It must be signed by one of issuer's current issuing keys.

Example:

```json
{
  "alg": "EC",
  "keyId": "issuing",
  "signature": "MEUCIQDCntn4GKNBja9LYPHa5U7KSQPukQYwHD2FuxXmC2I2QQIgEdN3EtFZW+k/zOe2KQYjYZWPaV5SE0Mnn8XmhDu1vg4=",
  "operation": {
    "issueCredential": {
      "credentialData": {
        "issuer": "7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39",
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

The returned identifier is hex encoding of the hash of the binary representation of `AtalaOperation` message. It is used to refer the credential in the revocation operation.

### RevokeCredential

It must be signed by one of issuer's current issuing keys.

Example:

```json
{
  "alg": "EC",
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

RPC response:

```json
{}
```


## Key management
As the users of the systems are the owners of their data, **they are responsible for storing their private keys securely**.

There are several details to consider, which depend on the user role.

**NOTE**: This section details the ideal key management features which can't be supported by December.

**NOTE**: The Alpha proposal doesn't include any key management.

### Issuer
The issuer must have several keys, which are used for different purposes:

1. A **Master Key** which can revoke or generate any of the issuer keys, this should be difficult to access, it's expected to be used only on extreme situations. A way to make it difficult to access could be to split the key into N pieces which are shared with trusted people, in order to recover the master key, any K of these pieces will be required. Compromising this key could cause lots of damage.
2. A **Issuing Key** which is used for issuing credentials, ideally, it will be stored in a hardware wallet, the Shamir's Secret Sharing schema could be adequate if issuing certificates is only done a couple of times per year. Compromising this key could lead to fake certificates with the correct signatures, or even valid certificates getting revoked (unless there is a separate key for revocation).
3. A **Communication Key**, which is used for the end-to-end encrypted communication between the issuer and the student's wallet, this can easily be a hot key, Compromising this key could not cause much damage.
4. An **Authentication Key**, which is used for authenticating the issuer with IOHK. Compromising this key could lead the attacker to get anything IOHK has about the issuer, like the historic credentials, and getting new connection codes. This key will ideally be stored in a hardware wallet, or an encrypted pen-drive.


Key storage:

- The issuer private keys are the most powerful because they can issue and revoke certificates, hence, if these keys get compromised, lots of people could be affected.
- The issuer ideally will handle their private keys using a hardware wallet (like ledger/trezor), but **this won't be supported by December**.
- Another way is to use an encrypted pen-drive to store the keys, but that should be done outside of our system.
- Shamir's Secret Sharing is the most common approach to do social key recovery, and while Trezor already supports it, **it can't be done by December**.


### Holder
- While the holder private keys are very important, they are less important than the issuer keys, compromising a user key only affects himself.
- The mobile wallet generates a mnemonic recovery seed which the holders should store securely, the keys on the wallet are encrypted by a password entered by the holder after running the wallet for the first time.

### Verifier
- The verifier keys have the less powerful keys, as they are only used to communicate with the holder wallet, getting them compromised doesn't allow the attacker to do much with them, they should be threaded with care but it is simple to recover if they get compromised.
- The verifier will ideally use an encrypted pen-drive to store their private keys but even a non-encrypted pen-drive may be enough.



## More
- The 2nd layer could work like a permissioned ledger on top of a public ledger, we could ask the issuers (trusted actors) to provide us their bitcoin addresses and process operations only from trusted addresses. This approach is similar to what OBFT does, it just accepts node holding keys on the genesis whitelist.

- Trezor or Ledger could be easily used for signing the issuer requests if we sign SHA256 hashes only, let's say, before signing any payload, its SHA256 is computed and that's signed. While this could be handy for testing but it breaks the purpose of hardware wallets, which is that they'll keep your keys safe even if your computer is compromised cause the SHA256 will be displayed before signing requests but the device owner doesn't have any warranty that the SHA256 was produced from its expected payload.


## Attacks and countermeasures

### Late publish attack

Malicious issuer, running its own node, can create an Atala Block including key change operation and reference it in Bitcoin blockchain without actually making it available via the Content Addressable Storage. In following blocks they can fully publish operations signed with such key - such as credential issuance or further key changes. In the future they can make the block available, invalidating all the operations made after that.

We don't have a solution for that problem - but the protocol assumes some level of trust towards the issuer anyways. If they want to make a credential invalid they don't need to launch any attacks - they can just revoke it.

The name _Late publish attack_ comes from https://medium.com/transmute-techtalk/sidetree-and-the-late-publish-attack-72e8e4e6bf53 blogpost, where it has been first described (according to our knowledge).

### DoS attack

Attacker might try to disrupt the service by creating blocks with large amount of operations. To counteract that limits are imposed on the number of operations in the block.

Another way of slowing down the network is creating many late publishes, forcing frequent state recomputation. In order to avoid such situation, in the future ATALA Blockchain can be made permissioned, so only approved nodes can publish blocks.

### Replay attack

Adversary might try to fetch an existing operation from Atala Block and send it again, e.g. re-attaching keys that were previously compromised and removed from the DID by its owner. Such strategy won't work in ATALA Blockchain, as each modifying operation contains hash of previous operation affecting the same entity.

### Hide-and-publish attack

In this attack malicious node receives an operation from an issuer, includes it into an ATALA Block and publishes a reference to the generated object in the Bitcoin ledger, but doesn't publish the ATALA Block or Object. The issuer is unable to know if their operation has been included into the block.

In such case they should send the operation again to another node - and it will be published. If the malicious node publishes their block, the new operation will become invalid (as it refers to the hash before first attempt to submit the operation, which is no longer the latest one), but as it is identical to the former, they have the same hash, so following operations won't be affected.

On the other hand when the client doesn't re-publish the exact same transaction, but publish different one instead, for example updating credential timestamp, the situation is much worse. When the malicious node publishes the old transaction, the following new one becomes invalid. Moreover, it has different hash, so whole chain of operations possibly attached to it becomes invalid. In case of very late publishes, days or even months of operations might be lost.

To avoid such situation implementation of issuer tool needs to make sure that if an operation has been sent to any node, it won't create any new operation affecting this entity until the original one is published.
