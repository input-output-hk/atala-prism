# Subject DIDs on credentials

As mentioned [before](selective-disclosure-merkle-trees.md#hidden-data-and-credential-ownership-proofs),
we currently verify that a credential belongs to a contact by relaying to a face to face identification.
For example, we expect to add a picture, or biometric data in credentials, and we expect to check that a 
person actually matches this data. However, we could make use of DIDs and cryptography to automate this
process.

If we add a DID to the `credentialSubject` field of our credentials (using the `id` field as specified in the 
[W3C data model](https://www.w3.org/TR/vc-data-model/#credential-subject)), we could provide cryptographic tools
to credential subjects. Once we add these DIDs, we could request at credential sharing time, a proof that the 
holder sharing the credential actually controls the DID that appears on it.

We need to describe interaction steps that involve a holder and a recipient. We assume that both parties have a trusted
channel, and that there is a unique identifier for the recipient, e.g. a DID `DID_R`, for which both parties agree that 
no other entity would be able to impersonate. Another example could be something like the recipient name (e.g. University
ABC).

For the description of the steps, we will assume that the identifier of the recipient is a DID `DID_R`.
 
The holder would like to share a credential `C` to the recipient. `C` contains a DID `DID_C` in its `credentialSubject` 
field. The interaction between the parties should allow the holder to:
- Prove to the recipient that he has control over `DID_C`, meaning that `C` was issued to him
- Avoid the recipient to impersonate the holder (in a different interaction with other party)

Note that the second point is important to forbid a man-in-the-middle situation.

The proof steps are:
- the recipient, with trusted identifier `DID_R` sends a nonce `N` to the holder.
- the holder can now send to the recipient the credential `C`, and a signature of `N || DID_R` (the concatenation
  of the received nonce and the recipient DID). The signature includes the key id used associated to `DID_C`. 
- the verifier concatenates his identifier (`DID_R`) to the nonce he shared and validates the received signature.

In this way:
- The holder generates a proof that will be accepted by the recipient (because the recipient generated the nonce `N` and
  the signature is produced with a key from `DID_C`)
- The recipient will not be able to impersonate the holder against other actor, because, even if the other actor also 
  uses `N` as nonce, the signature is tied to the `DID_R` DID. Recall that we have a secure channel assumption, and that
  we also assume `DID_R` to be a trusted identifier for the recipient. Therefore, the intended recipient could not 
  impersonate the holder against the third actor.


## Note related to linkability

The exchange of messages described above has a disadvantage for the holder. It leaves non-reputable proof that he 
interacted with the recipient (the signature of `N || DID_R` uses a key associated to `DID_C`). 

We have analyzed that, in order to fix this problem, we would need to move to an approach based on zero knowledge proofs.
ZK approaches tend to prove that the holder knows a secret associated to the credential that only the real holder knows.
We are evaluating the implications for a future iteration. 

## Work to do

- We need to update the mobile apps to generate DIDs for contacts (subjects/holders)
- We need to update the credentials SDK to add the `id` property inside the `credentialsSubject` (in the current design,
  this actually may not require changes in the SDK not backend)
- We need to implement the protobuf messages to represents the steps of interaction we described above

