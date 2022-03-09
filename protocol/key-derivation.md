<!-- This is meant to be part of a larger document -->

\newpage

# Key derivation and account recovery process

## Context

This document describes the process used to derive keys from a given seed and recover account information.
A user account is composed of mainly three parts:

- Cryptographic keys: Keys used by a user to control his communications and identity
- DIDs: Decentralised identifiers created and owned by the user
- Credentials: Credentials issued, sent and/or received by the user

The three parts of an account are stored in different places.
- First, cryptographic keys are stored in user wallets. During registration, each user generates a master mnemonic seed
  based on [BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki). The seed is known only by the user 
  and it is the user's responsibility to store this seed securely. We currently have no mechanism to recover an account
  if a user loses his seed. From that seed, we generate keys based on
  [BIP 32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki).
- Second, based on the cryptographic keys, users create their DIDs. DIDs are posted on atala files which are anchored in
  the underlying blockchain by the node. Associated DID Documents and events that update them are also stored off-chain 
  and anchored on the ledger by atala operations. 
- Third, credentials are stored by different components, specifically:
     - The credentials manager for the case of issuers and verifiers, and
     - The mobile wallet in the case of other users.
  
  For all users, shared credentials are also stored (encrypted) in the connector.

The account recovery process that we are describing, applies to solve extreme cases, such as hardware damage on clients' 
side. For example, when a user's computer/phone is lost, stolen or breaks down.

In the following sections, we will first describe the process of keys and DIDs generation. Later on, we will explain 
how to recover the different parts of an account.

## Definitions

Throughout this text, we will use definitions extracted from 
[BIP 32 conventions](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#conventions)

We assume the use of public key cryptography used in Bitcoin, namely elliptic curve cryptography using the field and 
curve parameters defined by [secp256k1](http://www.secg.org/sec2-v2.pdf). 

Given a initial (public, private)-key pair `m`, that we will call key master seed, we will define a function that 
derives a number of child keys from `m`. In order to prevent all keys depending solely on the key pair itself, the BIP 
extends both private and public keys first with an extra 256 bits of entropy. This extension, called the **chain code**.
The chain code is identical for corresponding private and public keys, and consists of 32 bytes. We refer to these 
(key, chain code) pairs as _extended keys_.
- We represent an extended private key as `(k, c)`, with `k` the normal private key, and `c` the chain code. 
- An extended public key is represented as `(K, c)`, with `K  = point(k)` and `c` the chain code. Where `point(p)`
  returns the coordinate pair resulting from EC point multiplication (repeated application of the EC group operation) 
  of the secp256k1 base point with the integer `p`.

Each extended key has `2^{31}` normal child keys, and `2^{31}` **hardened child** keys. Each of these child keys has an 
index.
- The normal child keys use indices `0` through `2^{31}-1`
- The hardened child keys use indices `2^{31}` through `2^{32}-1`. To ease notation for hardened key indices, a number 
  `i'` represents `i+2^{31}`

Hardened keys present different security properties that non-hardened keys.

## Generation process

### Root key generation

From [BIP39 spec](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed), the user 
generates a mnemonic seed that can be translated into a 64 bytes seed.

> To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic sentence (in UTF-8 NFKD) used as
> the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD) used as the salt. The iteration count is set
> to 2048 and HMAC-SHA512 is used as the pseudo-random function. The length of the derived key is 512 bits (= 64 bytes). 

From that 64 bytes seed, we will derive an extended private key that we will note with m.
Given the initial seed, libraries will provide a `generate` method to obtain m. We will refer to m as the root of our 
keys. 
 
### Children key derivations

BIP32 spec presents [three functions for extended key derivation](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#child-key-derivation-ckd-functions):
- CKDpriv((k, c), index): Given an extended private key and an index (>= 0), returns a new "child" extended private key.
- N((k, c)): Given an extended private key, it returns the corresponding extended public key (point(k), c)
  In order to derive the ith child extended public key from an extended private key (k, c), we perform 
  N(CKDpriv((k, c), i))
- CKDpub((K, c), index): Given an extended public key and an index, returns a new "child" extended public key.
  We won't use this last function

Comments
- Note that it is not possible to derive a private child key from a public parent key.
- We refer to these child keys as _nodes_ because the entire derivation schema can be seen as a tree with root m.
- Each node can be used to derive further keys. This allows to split the different branches of the tree and assign 
  different purpose for each one of them.
- **Notation:** We will use path notation, meaning that instead of `CKDpriv(m, i)` we will write `m / i`. For example,
  the expression `CKDpriv(CKDpriv(CKDpriv(m, 0'), 2'), 5')` translates to `m / 0' / 2' / 5'`. Recall that `i'` means 
  `i+2^{31}`.
- Libraries typically use path notation.  

For security reasons we will only use hardened child keys. For more details on this decision, read 
[this link](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#security) .

### The paths used in our protocol

Given our root key m, generated from a mnemonic seed, we will structure our derivation tree according to the following 
path.

```
m / DID_NUMBER / KEY_TYPE / '
```

where

- `DID_NUMBER` is a hardened node i' and represents the (i+1)th DID of a user.
- `KEY_TYPE` is one of:
  - 0': Representing master keys. The keys derived from this node are the only keys that can be used as MASTER_KEYs in 
        users' DIDs.
  - 1': Representing issuing keys.The keys derived from this node are the only keys used for credentials and operations 
        signing.
  - 2': Representing communication keys. The keys derived from this node are keys used to exchange establish connections
        and exchange messages with other users.
  - 3': Representing authentication keys. The keys derived from this node are keys used to authenticate with websites
        and servers. 
- The final `'` means that all derived keys must be hardened keys

NOTE: The `KEY_TYPE` list may be updated as we progress with the implementation.

Examples

- m / 0' / 0' / 0' is the first master key for the first DID of a user derived from m
- m / 0' / 0' / 1' is the second master key derived for the first DID for a user derived from m.
- m / 1' / 0' / 0' is the first master key for the second DID derived from m
- m / 3' / 0' / 5' is the sixth master key derived for the fourth DID derived from m.
- m / 1' / 1' / 0' is the first issuing key for the second DID derived from m
- m / 3' / 1' / 1' is the second issuing key for the fourth DID derived from m

Note that all nodes and final keys are hardened ones.

**Terminology and notation**:
- Given a `DID_NUMBER` `n`, we refer to the master key `m / n' / 0' / 0'` as the **canonical master key of the DID**. 
  Every DID that we will generate has a unique canonical master key.
- We will use the term _fresh key_ to refer to a key that has not been marked as used already.
- Given a path `p` we say that a key `k` is _derived_ from `p` if there exist `i` such that `k = p / i`.
- We say that a key `k` is derived from a master seed `m` if there exists a path `p` with root `m` and index `i` such 
  that `k = p / i`.
 
 **Conventions**
 Whenever we generate a fresh key:
 - We will use hardened keys.
 - we will use keys in order. This is, we always use the key derived from the smallest non-used hardened index. For 
   example, with respect generating a fresh issuing key for seed `m` and DID `d`, we will always pick the key generated
   by the smallest `i` where ` m / d' / 1' / i'` hasn't been used.                      

### DID generation

We would like to obtain a DID based from the mnemonic seed too. For this, we will fix a format for our DID Documents. 
When a user wants to create his first DID Document, the following process will be followed:

- Take `cmk = m / 0' / 0' / 0'`. This is, the canonical master key for the first DID derived from m.
- Mark this key as used for future reference.
- Create a DID Document that only contains `cmk` as a master key as its only field. The `keyId` field of the key 
  object will be `master-0`.
  We call this document the _canonical document associated to `cmk`_
- Send a CreateDID request to the node.
 
The request will return the DID suffix of the canonical document. We refer to `did:atala:[returned suffix]` as the 
_canonical DID_ associated to `cmk`.

To create the second DID, we now follow these steps:
- Take `cmk = m / 1' / 0' / 0'`. This is, the canonical master key for the second DID derived from m.
- Mark this key as used for future reference.
- Create a DID Document that only contains `cmk` as a master key as its only field. Again, with `keyId master-0`.
  We call this document the _canonical document associated to `cmk`_
- Send a CreateDID request to the node.

And, in the general case. If a user wants to create his (n+1)th DID:
- Take `cmk = m / n' / 0' / 0'`. This is, the canonical master key for the (n+1)th DID derived from m.
- Mark this key as used for future reference.
- Create a DID Document that only contains `cmk` as a master key as its only field, with `keyId master-0`.  
  We call this document the _canonical document associated to `cmk`_
- Send a CreateDID request to the node.

DIDs are generated in order and no keys can be shared between DIDs. This is, in order to create a new DID, we always 
pick the minimal N such that `m / N' / 0' / 0'` hasn't been used. No DID can use a key from a branch of the tree that 
does not derived from its own `DID_NUMBER`. 

### DID Document updates

In order to update the DID Document and add more keys. The keys should also be generated following similar processes as
the one for generation.
As examples:
1. Imagine a user owns 2 DID Documents D1 and D2 with corresponding canonical master keys k1 and k2 and DID numbers 0 
   and 1. Assume that D1 also has a non-canonical master key NK1. Graphically
   ```
   D1 = { keys : [ { keyId: "master-0", type: "Master", id: "k1", key: ... }, { keyid: "master-1", type: "Master", id: "NK1", key: ...}]
   D2 = { keys : [ { keyId: "master-0", type: "Master", id: "k2", key: ... }]
   ``` 
   To add a new master key to D2 we need to derive the next fresh key from `m / 1' / 0'` which is `m / 1' / 0' / 1'`
   and add it to D2 through an UpdateDID operation, the `keyId` used will be `master-1`. 
2. To add the first issuance key to D1, we should derive the next issuing key from `m / 0' / 1'` which is 
  `m / 0' / 1' / 0'` and add it to D1 through an UpdateDID operation, the `keyId` used will be `issuance-1`.

By convention, we will always have that the Nth key added of a given `KEY_TYPE` to a DID Document will be 
`[KEY_TYPE]-[N-1]`. Applications can present uses with local aliases in the front-end. We won't be concerned about
recovering such aliases in this document.

## Account recovery

Given the generation and derivation rules. The process to recover an account can be model as follows.

### DID recovery

Given that DIDs are generated in order based on canonical master keys. We can use the following process.
Given a master seed m

1. Set `i = 0`
2. Compute `cmk = m / i' / 0' / 0'`
3. Compute the canonical DID associated to `cmk`
4. Resolve the canonical DID
   1. If the DID can't be resolved: STOP
   2. If the DID is resolved, then:
        - Store tuples (i, KEY_TYPE, key) for each key present in the resolved DID Document for future steps (do not 
          store cmk)
        - Store cmk as a recovered master key
        - Store the canonical DID as a recovered DID
        - Increase i by 1 and go to step 1
        
This process will end up with all generated DIDs and their canonical master keys stored. 
It will also compute the set of keys present in the DID Documents that we will attempt to recover in the next step of
account recovery. 

### Key recovery

Note: This process assumes that all keys in the DID Documents owned by the user, were derived by the initial seed m.
While recovering all DIDs, we computed a set of (DID_NUMBER, key, KEY_TYPE) tuples that need to be recovered. Let's call
this set `KTR`. 
Given that we derive keys in order, we can recover all listed key in the following way.

1. Group tuples in `KTR` and group them by `(DID_NUMBER, KEY_TYPE)`. This will give us a set of keys that belong to the
   same DID and have the same KEY_TYPE. We will note each one of this set `KTR(DID_NUMBER, KEY_TYPE)`
2. For each set `KTR(i, kt)` from step 1. Derive the first `size(KTR(d,kt))` hardened keys from `m / i' / kt'`. This is
   the set `{ m / i' / kt' / 0',..., m / i' / kt' / (size(KTR(i, kt)) - 1)' }`
3. The keys from step 2 MUST provide us matching private keys corresponding to the ones in the `KTR(i, kt)` sets.
4. Mark the recovered keys as used for future reference.

We would like to remark that the process could be adapted to allow external keys in our DID Documents.
The process would follow the same steps with the modification that, in step 2, we will generate keys until we find the 
first one that does not match a public key in the set `KTR(d,kt))`. Also, in step 4, only keys with matching public key
in a DID Document will be marked as used.

### Credentials recovery

Now that we have explored how to recover all DIDs and keys present in them. The remaining part of the account that we 
need are the credentials relevant to the user.
Issuers and verifiers currently have their credentials in the credentials manager. Holders can find them in the 
connector. The recovery process will consist of generating keys used to communicate with services (e.g. authorization 
keys) from the master seed and call adequate APIs to get credentials stored by these components.

Note that the above process should be updated according to implementation updates.

### Test vectors

Test vectors are available as [JSON file](key-derivation-test-vectors.json).

#### Vector 1

Seed phrase: abandon amount liar amount expire adjust cage candy arch gather drum buyer
  * [DID: 1]
    * DID: did:prism:6fe5591aabaf1e41744f074336001f37be74534c00a99c3874c3a4690981dced
    * [Key master 0]
      * Key ID: master-0
      * BIP32 Path: m/1'/0'/0'
      * Secret key
        * (hex): dd7115d710d2eaa591f241145ebead016f7a2b90f2b86f5f743731fe37aa3ac5
      * Public key
        * (hex): 03cba11a413c631c853685bfd852b3163ffb124c03712f4a81cd115f72d6ced9f9
        * (x-hex): cba11a413c631c853685bfd852b3163ffb124c03712f4a81cd115f72d6ced9f9
        * (y-hex): 69278cf55b5d72ea6ad01f1a14787c2ee316cbe8f96897c6f8e9b23b13efe565
    * [Key master 1]
      * Key ID: master-1
      * BIP32 Path: m/1'/0'/1'
      * Secret key
        * (hex): 3c10eeba06cd6efefe017146fdf400d7e78b9fa461a6c7cd953e1fe235d35416
      * Public key
        * (hex): 03cdd203ac26fbc3282abd9a422558a3185371a27406164e2433a155a7bf901fa8
        * (x-hex): cdd203ac26fbc3282abd9a422558a3185371a27406164e2433a155a7bf901fa8
        * (y-hex): e9b72fe04894b19a8931d483a6b0979e95e3bbb34f786b6ac8512199989d2703
    * [Key issuing 5]
      * Key ID: issuing-5
      * BIP32 Path: m/1'/1'/5'
      * Secret key
        * (hex): f3222b9f1eeea1c11ceaaf39ff428c140c0f03e323d82d13ba94ca067ef1b79b
      * Public key
        * (hex): 0208e12a3029d8e6635ed40788250014831d47f58ed9e9ff5ddb217ab9fe931c09
        * (x-hex): 08e12a3029d8e6635ed40788250014831d47f58ed9e9ff5ddb217ab9fe931c09
        * (y-hex): 2eb3147e1c0d93908189f708657ac35b4e06a563b98bf09731d997be719a0d5e
    * [Key communication 20]
      * Key ID: communication-20
      * BIP32 Path: m/1'/2'/20'
      * Secret key
        * (hex): f8047dd871d8e79f54d9f202910eba779e67761d60b1b09ad1b8e72f2acbab2d
      * Public key
        * (hex): 02c10a63a773514bebfc8c9425737963ed135936172cec6aa13c9777201ffac50c
        * (x-hex): c10a63a773514bebfc8c9425737963ed135936172cec6aa13c9777201ffac50c
        * (y-hex): 58eaf1c1ed59a0eb56525498aabf8256e93953baca0c320c24827ea203b33ec4
    * [Key authentication 27]
      * Key ID: authentication-27
      * BIP32 Path: m/1'/3'/27'
      * Secret key
        * (hex): 0251a9f0c65de414c9522834afe62806d8c1a538c5282abbebf51f9f92f1eab5
      * Public key
        * (hex): 02ae26207160333e91fad88529b784bdbd9cd1a95e5ab6bbdc93ef289fa617c72e
        * (x-hex): ae26207160333e91fad88529b784bdbd9cd1a95e5ab6bbdc93ef289fa617c72e
        * (y-hex): c48c13b927248aab32874c0506dd26b6a90ef8452ab86147a4317045aed8f958
  * [DID: 17]
    * DID: did:prism:60e5c0b68701bac49873bc273017ad199a063e1b614444312dd2e97e1e9fb164
    * [Key master 0]
      * Key ID: master-0
      * BIP32 Path: m/17'/0'/0'
      * Secret key
        * (hex): 038ff9d6e6830ca7f5e875d4400c2a9f973551cafc7b077e5dfb23e49eb13e3f
      * Public key
        * (hex): 023d372976f436182400d21c07404b6d42fb87f84e59b5a7c715025cf85a5a3362
        * (x-hex): 3d372976f436182400d21c07404b6d42fb87f84e59b5a7c715025cf85a5a3362
        * (y-hex): dd0be8438eacd02d82e2e0c3069b20f71bda8e9f57a49183f73f4f4c127435d4
    * [Key master 17]
      * Key ID: master-17
      * BIP32 Path: m/17'/0'/17'
      * Secret key
        * (hex): 415590b46f2e4b0453da62328b8701becdacffd582fcd1008973774589199457
      * Public key
        * (hex): 02855112018b81d80d0187480fee241d0abf38e2f80dcab0039344f1b4a97cb7ab
        * (x-hex): 855112018b81d80d0187480fee241d0abf38e2f80dcab0039344f1b4a97cb7ab
        * (y-hex): 5dcc4c3877189fceaf1809aed0fd4491b900ed48b637f8529bc91bd305787b1c
    * [Key authentication 0]
      * Key ID: authentication-0
      * BIP32 Path: m/17'/3'/0'
      * Secret key
        * (hex): 7da8202ee5c58aebe3e3b1003c5217c8e9a841ff5da9e6358010635ed126383c
      * Public key
        * (hex): 03f6ede796792f949807db272c40f451811faf0f7eecb7fb2c6c0fd15d1c62b778
        * (x-hex): f6ede796792f949807db272c40f451811faf0f7eecb7fb2c6c0fd15d1c62b778
        * (y-hex): 217cb6a49fce5b20e646015422d7ad7c9c70ff7e6f2d02a1228eaff0ada9d66d
    * [Key authentication 17]
      * Key ID: authentication-17
      * BIP32 Path: m/17'/3'/17'
      * Secret key
        * (hex): 9f60801c2bf70b18071e8dd2d01d851a5e15602a407a79ff110e6dfb8371e6ce
      * Public key
        * (hex): 021b22881120925cf10381f5a247634665a677f98505bf183726a9b90f245a8a95
        * (x-hex): 1b22881120925cf10381f5a247634665a677f98505bf183726a9b90f245a8a95
        * (y-hex): 5a1b2feb44de35e5071810f931b8b4cf93bb6103eb86665563c65e120f6d9bc8

## Future challenges

A problem for a near future is that we would like to enable users to create DID Documents that contain more than just
the canonical master key. This can be achieved today by creating a canonical DID and then performing an update
operation. Given the metadata space we have, we could batch the two operations in a single Cardano transaction. If we
opt for this approach, the recovery process would work without the need of changes.

As an alternative, we could generate initial DID documents with more keys and data than the canonical master key and 
back up them in some external storage. The recovery process would then iterate upon the documents and re-generate the
keys associated to them. 

Another alternative could be to extract from an initial state of DID Document its _canonical_ part, this is the initial
master key associated to the DID. The DID suffix could be dependent only on the canonical part. For example, an initial
DID Document could be:

```
{
  "keys": [
    { 
      "id": "master0",
      "type": "MASTER_KEY", 
      "key": ... 
    },
    { 
      "id": "issuance0",
      "type": "ISSUING_KEY", 
      "key": ... 
    } 
  ] 
}
```

where the canonical part is the DID Document 

```
{
    "keys": [
        { 
          "id": "master0",
          "type": "MASTER_KEY", 
          "key": ... 
        }
    ]
}
```

The DID suffix could be computed from the canonical part (its hash). This could allow for published DIDs to have a 
generic initial state without breaking our recovery process. However, the non-canonical part does not become self
certifiable anymore. Meaning that for an unpublished DID, a user could have the DID Documents:

```
{
  "keys": [
    { 
      "id": "master0",
      "type": "MASTER_KEY", 
      "key": ... 
    },
    { 
      "id": "issuance0",
      "type": "ISSUING_KEY", 
      "key": ... 
    } 
  ] 
}
```

and 

```
{
  "keys": [
    { 
      "id": "master0",
      "type": "MASTER_KEY", 
      "key": ... 
    },
    { 
      "id": "master2",
      "type": "MASTER_KEY", 
      "key": ... 
    } 
  ] 
}
```

and both would have the same short form (canonical) DID.
The approach still would not solve the recovery of [unpublished DIDs](./unpublished-dids.md).

We have also mentioned that the node could expose an endpoint to obtain DIDs based on a key. This could also allow a
recovery process that remains compatible with a generic initial DID Document. 

The above non-extensive analysis suggests the need of external storage for DID recovery or to split the creation of
complex DID Documents into a Create and an Update part.
