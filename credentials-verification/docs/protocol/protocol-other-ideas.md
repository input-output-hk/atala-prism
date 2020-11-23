<!-- This is meant to be part of a larger document -->

\newpage

# Improvement proposals

This document contains ideas we would like to evaluate for future releases.
  

## Make use of the underlying Cardano addresses

Our operations require signatures that arise from DID related keys.
Apart from this signature, an operation is attached to a transaction that will also contain a signature related to its
spending script. We _may_ be able to reduce the number of signatures to one.

Sidetree has update commitments (idea that probably comes from KERI[1](https://www.youtube.com/watch?v=izNZ20XSXR0))
[2](https://arxiv.org/abs/1907.02143). The idea is that, during DID creation, the controller commits to the hash of a 
public key. Later, during the first update, the operation must be signed by the key whose hash matches the initial 
commitment. The update operation defines a commitment for the next operation. These keys are part of the protocol and 
are optionally part of the DID Document.

Now, here is an improvement we could use:

- The initial DID could set a commitment to a public key hash PKH1 (as in KERI and Sidetree). We call this, the genesis
  commitment.
- Now, in order to perform the first update, the controller needs a UTxO to spend from. Let the user receive a 
  funding transaction to an output locked by PKH1. Let us call this UTxO, U1.
- The update operation could now add metadata in a transaction that _spends_ U1. The transaction signature will be 
  required by the underlying nodes, meaning that we could get signature validation for free. This transaction could also
  create a new single UTxO, that we could name U2. This output, could again be a P2PKH script representing the next 
  commitment.

We could define similar "chains" of transactions for DID revocation, credential issuance (declared in an issuer DID), 
and possible credential revocation registry.
  
Some observations of this approach are:

- Positive: We get smaller metadata.
- Positive: Light node ideas described in the section [below](#light-nodes-and-multi-chain-identity).
- Positive: We may be able to get "smarter" locking scripts for operations relaying on the underlying chain scripts.
- Negative: It becomes more complex to do "on-chain batching" for DID updates. We may be able to use a multi-sig script
  for this. Credential batching remains unaffected. Recall that on-chain batching may be considered bad behaviour, see
  [metadata usage concerns](./protocol-v0.3.md#metadata-usage-concerns)
- Negative: One would need to do the key/transaction management a bit more carefully. The key sequence could be derived
  from the same initial seed we use.

## Light nodes and multi-chain identity

We are having conversations with the research team and Cardano teams to suggest a light node setting for PRISM.
Assume we could:
1. Validate chain headers without downloading full blocks.
2. Find in each header an structure like a bloom filter with low (around 1%) false positive response that tells if a 
   UTxO script has been used in the associated block. Note: our proposal is to add a hash of a bloom-like structure and
   not the filter itself.

With those two assumptions, if a light node has the full chain of headers, then given a DID with its initial state, the
node could:

1. Check all the bloom filters for the one that spends the initial publish key hash we described in the previous section.
   If no bloom filter is positive, then the current DID state is the one provided.
   If K filters return a positive response, we could provide the script to a server which should provide either:
     + A transaction (with its metadata) with its merkle proof of inclusion in one of the positive blocks, or
     + The actual blocks matching to those filters so that the node can check that all were false positives.

The addition of the bloom filter is important because it mitigates the typical SPV problem of a node hiding data.

We may be able to follow a similar process for credential issuance and revocation events.
We could also check on real time for updates based on the headers for those DIDs for which we already know about.
For this process we just need to know a genesis commitment.

We should check for possible spam attacks. E.g. providing an address with many matches. We have been referenced to 
[section `TARGET-SET COVERAGE ATTACKS`](https://eprint.iacr.org/2019/1221.pdf) and [this attack on Ethereum 
filters](https://medium.com/@naterush1997/eth-goes-bloom-filling-up-ethereums-bloom-filters-68d4ce237009). Given the
small size of Cardano blocks, we should evaluate the number of UTxOs and transactions an attacker should create to 
saturate the filter. It should also be compared the optimal relations of filter size w.r.t. block size. 

### Bonus feature - multi chain DIDs

With a "light node" approach, we may be able to move from one chain to another while only downloading headers.
This may be achievable by posting an update operation that declares a change of blockchain and then search for the 
desired script in the other chain. 

We are not aware of blockchains that implement bloom filter like structures (or their hashes) in headers for UTxO 
scripts at the time of this writing. We haven't explored yet how to translate this approach to account based systems
like the Ethereum case.

## Layer 2 batching without late publication

To be expanded
- The basis is to register _publishers_ that could batch operations. The publishers could be whitelisted by IOHK.
- DID controllers could send an on-chain event registering to a publisher (useful if they plan multiple updates to the 
  same DID). The registration to a publisher could be part of the initial DID state too.
- From that point on, the associated updates for that DID could only be posted by the publisher.
- The publisher must publish file references signing the publication with his DID.
- If a publisher does not reveal a file `F1`, and publishes a file `F2`, then no node should process `F2` until `F1` is
  published. This prevents late publication because no DID is attached to many publishers.
- The controller can decide to post a _de-registration_ event at any point. It will be interpreted only if all previous
  files are not missing.
- If there is a missing file and the controller is still registered to the publisher, then its DID gets stuck. This seems
  to be, so far, the only way to avoid late publication.
- We have evaluated other ways to handle the situation where the publisher does not reveal the file. For example, to
  allow controllers to post on-chain the operation they tried to batch. The complexity arises because we should assume
  that the controller and publisher could collude (they could even be the same person). 
    - For example, we thought about having a merkle tree hash published with the file reference, to allow controllers to
      move on in case a file is not revealed. However, the publisher can simply decide to post a different merkle root
      hash or even to never provide the inclusion proof to the controller.
