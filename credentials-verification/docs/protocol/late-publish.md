<!-- This is meant to be part of a larger document -->

\newpage

# Late publication

So far, we have explained how the protocol can achieve scalability though a batching technique. However, it is important
to also consider other implications of this approach. We will describe a potential "attack" that is possible due to 
having off-chain data that is referenced from on-chain transactions. In short, the absence of control mechanisms to
manage off-chain data availability may allow an attacker to produce undesired situations as we will explain below. 

## The basic scenario

At this point, the implementation is not whitelisting who can publish Atala objects and blocks. Let's consider then, the 
following scenario, Alice decides to create a new DID `D1` with DID Document with key `k1`. She creates an Atala Object, 
`A1`, and a single operation Atala block, `B1`. She then sends a transaction, `Tx1`, to the underlying ledger with a 
reference to `A1` in its metadata and publishes the generated files in the CAS. All nodes will see `D1` with `k1` as 
valid since `Tx1.time`.

Now, imagine that Alice creates another Atala object, `A2`, and corresponding single operation Atala block, B2, that 
updates `D1` by revoking `k1`, she sends a valid transaction, `Tx2` (with a reference to `A2`), to the underlying 
ledger, but this time she does not post the files to the CAS. This should lead other nodes that are exploring ledger 
transactions, to attempt to resolve the reference in `Tx2` and fail. In order to avoid nodes to be blocked by this 
situations, we could mark `Tx2` with an `UNRESOLVED` tag and continue processing subsequent transactions. Nodes would 
keep a list of `UNRESOLVED` transactions and would keep attempting to retrieve missing files periodically until they 
succeed. Once a missing file is found, the node state should be properly updated (where properly means, reinterpret the 
entire ledger history and applying updates to the state incorporating the missing file operations). Note that we say 
that this _should_ happen, however, the current implementation assumes that the files are always published.

The outcome of the above process should lead all nodes to have the following state:

- A transaction `Tx1` which publishes `D1` with a DID Document that contains `k1` as valid key
- An `UNRESOLVED` transaction `Tx2` (which updates `D1` to revoke `k1`)

```
Ledger
   ----+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···
   ----+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1, 
 hash(B1) --> B1
]

Node State
unresolved = [hash(A2)]
dids = [ D1 --> [k1 valid since Tx1.time ] ]
```

Alice could now issue a credential `C1` signing the `IssueCredential` operation with `k1`, post it in files `A3`, `B3` 
with a transaction `Tx3` and publishing `A3`, `B3` in the CAS. Leading to:

```
Ledger
   ----+-----+---------+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···   | Tx3 |   ···
   ----+-----+---------+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1,
 hash(B1) --> B1,
 hash(A3) --> A3, 
 hash(B3) --> B3
]

Node State
unresolved = [hash(A2)]
dids = [ D1 --> [k1 valid since Tx1.time ] ]
creds = [ hash(C1) --> Published on Tx3.time ]
```

If Bob receives `C1` and validates it, the node will state that the credential is correct.

Later, Alice can post `A2` and `B2` to the CAS. This _should_ lead to a history reinterpretation (note that this is not
currently implemented) producing the state in all nodes as follows: 

```
Ledger
   ----+-----+---------+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···   | Tx3 |   ···
   ----+-----+---------+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1,
 hash(B1) --> B1,
 hash(A2) --> A2,
 hash(B2) --> B2,
 hash(A3) --> A3, 
 hash(B3) --> B3
]

Node State
unresolved = []
dids = [ D1 --> [k1 valid since Tx1.time, revoked in Tx2.time ] ]
creds = [ ] -- as the IssueCredential event was signed with an invalid key, C1 may not even be added to the credentials
            -- map
```

If Bobs tries to validate `C1` again, he won't only find that `C1` is invalid now. According to ledger history, `C1` was 
_never_ valid because it was signed with a key that was revoked _before_ `C1` was published. Note that, the only ways to 
notify Bob the change in state of `C1` are
  1. Bob periodically queries the state of `C1`.
  2. The node updates the history rewrite, replays all queries done by Bob and notifies him if any response would be
     different than the one he received before the history change. 

If there is no history rewrites, Bob could simply subscribe to a notification of a `C1` revocation event. As `k1`, in 
this example, is used to sign the operation, Bob could notify the node to inform him if the credential becomes invalid
after history rewrite. 
Furthermore, note that if `C1` did not support revocation, then this history rewrite could "in the real world" imply its
revocation after a proper validation.


## Comparative - situation 2: Attack after key revocation

Let us now imagine the scenario where Alice is not hiding files. She starts by publishing `Tx1`, `A1` and `B1` as before.
Imagine that now Alice suspects that her key was compromised and decides to revoke it by publishing `Tx2`, `A2` and `B2`.
The state reflected in the system from the perspective of a node would be:

```
Ledger
   ----+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···
   ----+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1, 
 hash(B1) --> B1
 hash(A2) --> A2, 
 hash(B2) --> B2
]

Node State
unresolved = [ ]
dids = [ D1 --> [k1 valid since Tx1.time, revoked at Tx2.time ] ]
creds = [ ]
```

If an attacker Carlos, who actually got control of `k1`, creates a credential `C1` and sends an `IssueCredential` event 
for `C1` in `Tx3`, `A3`, `B3`. We would get the state:

```
Ledger
   ----+-----+---------+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···   | Tx3 |   ···
   ----+-----+---------+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1,
 hash(B1) --> B1,
 hash(A2) --> A2,
 hash(B2) --> B2,
 hash(A3) --> A3, 
 hash(B3) --> B3
]

Node State
unresolved = []
dids = [ D1 --> [k1 valid since Tx1.time, revoked in Tx2.time ] ]
creds = [ ] -- C1 is not even added to creds map because the key to sign the IssueCredential operation was revoked
```

Note that in this situation there is no point in "real world time" where `C1` could have been valid, as Carlos was not
able to publish `C1` before `k1` is revoked. The point that we want to remark is: **_this ledger, CAS and node states
are identical to the ones where the late publish occurred_** 

## Comparative - situation 3: Attack after and later credential and key revocation

Let us now analyse a third situation. In this case, there is again no late publication. As in the last two scenarios,
we start with Alice publishing `D1` with valid `k1` through `Tx1`, `A1` and `B1`. Now imagine that Carlos gains access
to `k1` and issues `C1` though `Tx2`, `A2`, `B2` before Alice manages to revoke `k1`. The system state would look like
this:

```
Ledger
   ----+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···
   ----+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1, 
 hash(B1) --> B1
 hash(A2) --> A2, 
 hash(B2) --> B2
]

Node State
unresolved = [ ]
dids = [ D1 --> [k1 valid since Tx1.time ] ]
creds = [ hash(C1) -> Published on Tx2.time ]
```

If now Bob receives `C1` and verifies it, it will receive a response stating that the credential is valid.
Now, if Alice notices that a credential was issued without her knowledge, she could revoke `C1` and `k1` through a 
transaction `Tx3` with files `A3` and `B3` containing an `UpdateDID` and a `RevokeCredential` operations. Leading to the
state:

```
Ledger
   ----+-----+---------+-----+---------+-----+-----
  ···  | Tx1 |   ···   | Tx2 |   ···   | Tx3 |   ···
   ----+-----+---------+-----+---------+-----+-----

CAS
[
 hash(A1) --> A1,
 hash(B1) --> B1,
 hash(A2) --> A2,
 hash(B2) --> B2,
 hash(A3) --> A3, 
 hash(B3) --> B3
]

Node State
unresolved = []
dids = [ D1 --> [k1 valid since Tx1.time, revoked in Tx4.time ] ]
creds = [ hash(C1) -> Published on Tx2.time, revoked on Tx3.time ] 
```

Note that both the blockchain and the node state reflect the period of time in which verifying `C1` could have resulted
in an is valid conclusion. We could even extend the operations we support to add a `RevokeSince` to reflect that the 
intention is to revoke a credential since a block number/time previous to the revocation event and still keep the
record of precisely what happened on chain.

**Comment**

We would like to remind that, we wait for transactions to be in the stable part of the ledger before applying them to 
the node state. This means that there is a time period a user has to detect that an event triggered on his behalf is 
going to be applied. We could allow issuers to specify waiting times in some way to facilitate key compromised 
situations. E.g. a credential schema could be posted on chain specifying how many **stable** blocks the protocol needs 
to wait to consider a credential valid (to facilitate the issuer with time to detect compromised keys). If a credential
is issued but a revocation event is found (in a stable block) before the end of this waiting period (after the issuance
event), the protocol can then ignore the credential, never adding it to its state and, hence, improving the chance for
unauthorised credentials to never be valid. 

Another small improvement we could remark is, if a client queries data to a node, and the node can see in the 
**unstable** part of the blockchain that an event will affect the queried data, the node could reply requests with an 
extra flag pointing out the situation. It will be then up to the client to wait for a few minutes to see if data changes
are confirmed, or to simply ignore the incoming information.

## Some conclusions

A system that allows late publishing brings some complexities:

- Rollbacks and history rewrites should be handled properly
- Clients should have richer notification methods to understand what happened
- A priori, credentials that do not have a revocations semantic, could be revoked through late publication
- Auditability becomes a bit blurry
- It makes reasoning more complex if we expand the protocol with more operations
- As illustrated [here](https://medium.com/transmute-techtalk/sidetree-and-the-late-publish-attack-72e8e4e6bf53), 
  it also adds complexities to DIDs or credentials transferability.

Now, not everything is negative. Late publishing possibilities are a consequence of decentralization.

If one decides not to allow late publishing, the selected approach to do so may bring disadvantages:

- Whitelisting who can issue operations with batches
   + This leads to some centralization. It could be mitigated by allowing on-chain operation publishing (which is our 
     current plan). Whoever does not want to relay on IOHK, can issue their operations on-chain without batching. 
     They would need to trust that IOHK won't create a late publish scenario (which everyone would be able to audit).
   + We could also consider to give two references to our protocol files, one to S3 and one to IPFS. Batching 
     transactions would still be done only by IOHK. The node would try to get the file from IPFS first, if it fails it 
     would try IOHK CAS (currently S3), if both fail, then the node should stop and report the error. 
     In this way, the system would be less dependent on IOHK's existence because anyone could first post the file in 
     IPFS and then send it to us. In this way, even if IOHK disappears, the files could be supported online by other 
     entities making the DIDs generated truly persistent. The system would still remain functional without IOHK. This
     may also require less resources from IOHK as IPFS could reply to queries that would go to S3.
   + Unfortunately, the above idea still does not remove the possibility of IOHK performing a late publish attacks
     because IOHK could still post references to self generated blocks or by modifying Atala block sent by a user and 
     posting a reference on chain of a subset (or superset) of the block provided and not posting the file itself. 
     * We may be able to mitigate the subset case by requesting users to sign the list of all operations they intend 
       to batch in a single block (assuming all operations batched by a user are signed by single DID, which is the
       case of issuing a barch of credentials). Then we could request that signature in the batch as an integrity 
       requirement. This would not allow IOHK to publish a subset of the batch.
     * Note (as a consequence of the above point) that if we restrict batches to only contain events created by a single
       authority (i.e. all events in a batch are signed by the same key), we could request that the batch file must have a signature of the same key. 
       This removes any possibility for IOHK to perform late publish attacks on other users data. 
- A BFT CAS system that provides proofs of publication could be a great solution for us, but may be complex (if at all
  possible) to implement. 
- Allow legally bounded entities to also publish files could aid towards a semi-decentralized system.
- Once we implement rollbacks/history rewrites we could consider further decentralization if we see the need, but it
  would bring the possible auditability details or the issue with non-revocable credentials mentioned in the document.

Long story short. If we handle history rewrites, we can guarantee consensus of all nodes about the _current_ state of
the system. However, if we implement a way to remove late publication possibilities, we would also get consensus about
the _past history_ of the system. Different use cases may require different approaches.
