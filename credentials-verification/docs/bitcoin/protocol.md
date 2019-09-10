# Bitcoin 2nd layer protocol
This document describes a the protocol for building the credentials project as a 2nd layer on top of Bitcoin. The protocol is based on the [sidetree protocol](https://github.com/decentralized-identity/sidetree/blob/master/docs/protocol.md) from Microsoft.

## Definitions
The definitions from the [high-level-design-document](https://github.com/input-output-hk/cardano-enterprise/tree/develop/credentials-verification/docs/bitcoin#definitions) apply here, be sure to review those ones first.

- **DID**: A decentralized identifier, see the [official spec](https://w3c-ccg.github.io/did-spec/).
- **DID Document**: The document representing a state of a DID, includes details like its public keys.
- **ATALA Transaction**: An operation in the 2nd layer ledger (ATALA Node), like `Create DID`, `Create Credential`, etc, look into the [supported transactions](#atala-transactions) for more details. This is equivalent to a Bitcoin Transaction on the 2nd layer protocol.
- **ATALA Block**: A list of ATALA Transactions, includes metadata about the block. This is equivalent to a Bitcoin Block on the 2nd layer protocol.
- **ATALA Object**: A file that has metadata about an ATALA Block and a reference to retrieve it. A reference to an ATALA Object is the only detail stored in Bitcoin transactions.
- **Genesis Bitcoin Block**: The first Bitcoin Block aware of ATALA Objects, everything before this block can be discarded.

## Differences from sidetree
This section states the main known differences from the sidetree protocol:
- We support credentials while sidetree only supports DIDs.

## The path to publish an ATALA Transaction
There are some steps involved before publishing an ATALA Transaction, the following steps show what the ATALA Node does to publish a DID Document, publishing any other ATALA Transaction would follow the same process.

**NOTE**: The JSON formats below are illustrative, when building the actual features, we'll update them to what we actually need, trying to follow sidetree formats if is practical.

1- Assume we have a DID Document which gets to the ATALA Node, like:
```
{
  "@context": "https://w3id.org/did/v1",
  "publicKey": [
    {
      "id": "#key-1",
      "type": "Secp256k1VerificationKey2018",
      "publicKeyJwk": {
        "kty": "EC",
        "kid": "#key-1",
        "crv": "P-256K",
        "x": "ostQVNLv52D3eioe0lsMRNng6stDrvzPVpQI3n8UCww",
        "y": "BmwZQjOif6ON0jJ4vTQgmBhlcKmoQ_P8bdDXZUmY_Mw",
        "use": "verify",
        "defaultEncryptionAlgorithm": "none",
        "defaultSignAlgorithm": "ES256K"
      }
    }
  ],
  "id": "did:geud:EiB-xIxpyCt5N5n8zyorv3RUz9NDJgRqfkA_DWC0NRMlpg"
}
```

2- An ATALA Transaction is created, it represents the creation for the DID Document, like:
```
{
  "type": "PublishDID"
  "doucment": {
     "header":{
        "alg":"ES256K",
        "kid":"#key-1",
     },
     "payload":"eyJAY29udGV4d...",
     "signature":"MEQCIAFeHOtb2hcyaI..."
  }
}
```

3- An ATALA Block is created, it represents the list of transactions to publish on the Bitcoin blockchain, like:
```
{
  "version": "1.0",
  "transactions": [
    {
      "type": PublishDID",
      "data": {...}
    }
  ]
}
```

4- An ATALA Object is created, it links the previous ATALA Block and some metadata which can help the ATALA Node to choose whether to retrieve the ATALA Block or not (think about a light client), like:
```
{
  "blockhash": "[HASH(ATALA Block)]",
  "size": 1024,
  "transactions": 1
}
```


5- The ATALA Block and the ATALA Object are pushed to the object storage (S3, DAT, IPFS), each of these files are identified by the hash of its content, like `HASH(ATALA Object` and `HASH(ATALA Block)`.

6- A Bitcoin transaction is created and submitted to the Bitcoin network, it includes a reference to the previous ATALA Object on the special output (see the one with OP_RETURN), the `FFFF0000` is an magic value (to be defined) which tells ATALA Nodes that this transaction has an ATALA Object linked in it, after the magic value, you can see the `HASH(ATALA Object)`, like:
```
{
  "txid": "0b6e7f92b27c70a6948dd144fe90387397c35a478f8b253ed9feef692677185e",
  "vin": [...],
  "vout": [
    {
      "value": 0,
      "n": 0,
      "scriptPubKey": {
        "asm": "OP_RETURN FFFF0000 f2ca1bb6c7e907d06dafe4687e579fce76b37e4e93b7605022da52e6ccc26fd2",
        "hex": "6a2258...",
        "type": "nulldata"
      }
    }
  ],
  "hex": "01000000...",
  "time": 1526723817
}
```

7- After the published Bitcoin transaction has N (to be defined) confirmations, we could consider the transaction as final (no rollback expected), which could mark all the underlying ATALA Transactions as final.



## Updating ATALA Node state
The ATALA Node has an internal database where it indexes the content from the ATALA transactions, this is useful to be able to query details efficiently, like retrieving a specific DID Document, or whether a credential was issued.

The ATALA Node needs to have a Genesis Bitcoin Block specified, everything before that block doesn't affect the ATALA Node State.

These are some of details that the ATALA Node state holds:

- DIDs and their respective current document.
- Credentials (hashes only), who issued them and when they were revoked (if applies)


The component updating the ATALA Node state is called the Synchronizer.

The Synchronizer keeps listening for new blocks on the Bitcoin node, stores the Bitcoin block headers and applies rollbacks when necessary.

After storing a block header, we must look for ATALA Objects, retrieve the object from the storage, perform the validations (to be defined) on the ATALA Object, the ATALA Block, and the ATALA Transactions, and apply the ATALA Transactions to the current ATALA Node state.


There are several possible strategies to follow when validating the ATALA Object (we must eventually choose the one that works better for us):

1. The whole ATALA Object must be valid, any invalid ATALA Transaction leads to a rejected ATALA Object.
2. The ATALA Object must have the right format but invalid ATALA Transactions are tolerated and not applied to the state.



## ATALA Transactions
Here you can find the possible ATALA Transactions. The list will be updated when new transactions get implemented (there are no transactions implemented right now)

TODO.


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
- The 2nd layer could work like a permissioned ledger on top of a public ledger, we could ask the issuers (trusted actors) to provide us their bitcoin addresses and process transactions only from trusted addresses. This approach is similar to what OBFT does, it just accepts node holding keys on the genesis whitelist.

- Trezor or Ledger could be easily used for signing the issuer requests if we sign SHA256 hashes only, let's say, before signing any payload, its SHA256 is computed and that's signed. While this could be handy for testing but it breaks the purpose of hardware wallets, which is that they'll keep your keys safe even if your computer is compromised cause the SHA256 will be displayed before signing requests but the device owner doesn't have any warranty that the SHA256 was produced from its expected payload.
