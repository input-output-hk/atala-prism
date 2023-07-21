# Selective disclosure

## Context

In our current design, when a holder shares a credential, all the information stored in the credential is 
shared. This is not ideal for self-evident privacy reasons, the holder may like to prove a verifier a set
of sub-claims that are present in a credential instead of displaying all.

In this document, we will describe a first approach for improving our situation. We will add a way to 
hide claims inside credentials, and allow the holder to select which of those hidden claims will be
revealed to each verifier.

## Approach

We will allow the issuer of a credential to provide a list of claims (identified as `(name, value)` 
pairs) to our SDK. The pairs will be represented in the credential in the following way:
- Each pair will have a nonce unique associated to it (this is added to protect against brute force
  attacks for a given field), leading to tuples `(nonce, name, value)`
- The SDK will compute a merkle tree based on a serialization of these tuples
- We will store a merkle root in the credential, and return to the issuer the inclusion proofs and 
  nonces for each field
- The merkle root will be added as a claim inside the `credentialSubject` field of our current format
- We will keep outside the hidden fields values that represent issuer DID and signing keys

# Impact of the implementation 

We will need to update:
- Our SDKs to expose the described functionality for credentials construction
- We also need to add to the SDK, methods to verify the validity of exposed data from the tree
- The console backend to store the newly added merkle proofs and nonces for the hidden fields
- The protobuf models to share credentials, we need to add these new proofs, nonces and field names
- The mobile applications, to allow the selection of which fields should be shared
- The management console front end to manage this new data


## Remarks

### Hidden data and credential ownership proofs

One consideration about the possibility of hiding data, is that we are currently not adding a subject's
DID in our credentials. Our current requirements expect that the ownership of a credential will be 
verified by "classic" mechanisms, such us the insertion of a photograph of the subject, or biometric
data represented in custom formats. 

The approach of not having subject's DID in credentials, has an advantage on simplicity and avoids
problems related to key management on the subject's side. On the other hand, we should remark that the 
lack of cryptographic mechanisms restricts the use of automated non-human credential verification. For
example, if a DID is involved, a service could use it to verify that the holder presenting a credential
is also in control of the DID that is present in it. Without such DID, the service will need to rely on
other credential custom ways to verify that a holder is the actual subject of a credential.
In the context of selective disclosure, a holder may desire to prove a claim without the need of sharing
sensitive data (e.g. biometric data of identification photograph).

For the scope of this iteration, we will not add subjects' DIDs in credentials.

### Hidden data and credential templates

In our current implementation, we have HTML templates that display a credential with all its attributes.
If we add selective disclosure of fields (independently of the technique we choose to do so), we need to 
evaluate the impact on the visual presentation we have.

Today, we store the HTML templates _within_ the credential with all its fields populated. In order to
make sense of the feature of hiding fields, we could move the complete template to the hidden fields of
the credential. However, this would only allow us to share either the HTML view with all the fields
populated, or no HTML view at all.

An alternative is to design a presentation layer that could manage the dynamic creation of HTML views.
This layer would consume a service which abstracts how to retrieve data from a received credential.
This service would need to handle different formats of credential sharing. For example, in the current 
implementation, we share the entire plain credential. Retrieving data from a credential in this context,
means to be able to extract fields from based solely on the name/identifier of the field (this may 
require to standardize the naming conventions for credentials' fields). 
On the other hand, if we implement merkle trees for selective disclosure, the service would need to
handle how to retrieve data from the plain text part of the credential plus the leaves and inclusion
proofs. Similarly, if we iterate towards ZK techniques, the service will need to handle related 
structures too. All abstracted under a single interface.
 
Now, the proposal to abstract a layer that allows retrieving data from a received credential, is not
defining requirements upon the presentation layer itself. We leave the problem of constructing the HTML 
views themselves out of the scope of this document. We want to remark that a view may eventually depend
on multiple credentials, or even be a derived claim (as is the classic example of "above age" base on a
"date of bird" claim). The presentation layer for such claims needs proper refinement.

### Privacy limits

We want to remark that, even though this approach adds functionality that we do not support today,
i.e. some basic privacy, it still has flaws which we would like to document.

- Merkle tree based techniques allow correlating holders' activity. If a holder `H` shares one hidden
  field to a verifier `A`, and other field is shared to another verifier `B`, then `A` and `B` could 
  deduce that both pieces of data revealed correspond to `H`. This is because both verifiers can see 
  that the same credential was used (because the credential hash will be the same in both cases) 
- Note that the above, is not a consequence of the way in which we hide the fields, but a consequence
  of the fact that we share the entire credential to both verifiers. This is needed because we check
  the timestamp of the credential creation.
  
The points above are known for us, and we intend to iterate towards other cryptographic schemes to 
overcome these correlations. We remarked it here for completeness purpose.
