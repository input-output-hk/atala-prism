# Proposal: MoE controls all master keys

This document is an implementation guideline for the solution to the [endorsements requirement](./endorsements.md) based
on the approach that allows every institution to have their own DID, but the master key of each institution is under
control of the MoE.

Implications of this approach
- The MoE will need to sign operations as part of the process that allows updating keys of any institution. 
  This process would be executed every time an institution looses their keys, or when an attacker takes control of their
  `issuing keys`, i.e. the ones used to issue verifiable credentials and endorsements.
- We will add a new service to the MoE management system, called the "endorsements service".

Let's start with the description of the new service, which will manage the application level logic related to endorsements.

## Endorsement service

The `Endorsements service` is a new component in the system that we need to add for this proposal.
The service will be responsible for:
- Registering institutions in its database with roles, DIDs, and id information (more detailed information will live in
  other parts of the management system)
- Imposing conditions that allow an institution to endorse another institution (role based rules)
- Imposing conditions that allow an institution to revoke endorsements of another institution
- Keeping the history of endorsements (and their revocations)
- Providing an API that, given an issuer DID, should return the period in time for which that DID has been endorsed to
  issue verifiable credentials 

### Service state

The endorsements service state (at a high level) contains
- A set of trusted DIDs (the MoE DID, and the ones endorsed). This is the cornerstone upon which the source of trust of
  the entire system is built.
- A list of signed public keys provided by the MoE
  + Each key in the list will have a signature that comes from a key in the MoE DID (most likely the MoE's issuing key)
  + The list should consist of hardened child keys derived from an extended private key owned by the MoE
  + Note we cannot use extended public keys due to security implications (see [BIP 32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#implications))
- The relation between public keys and who requested them.
- For each trusted DID, it tracks:
  + the public key it uses as master key,
  + the DID that endorsed it,
  + the periods where the endorsement was valid, and
  + the issued verifiable credentials used for endorsements (of each period)

### Service initial state

As initial state, the service will have:
- The trusted set contains just the DID of the MoE. It will be considered trusted since the beginning of the system life
- A sufficiently large list of hardened public keys derived from an extended private key controlled by the MoE
- The associated signature to all the keys in the previous list
- No key associated to any DID
- No information for any other DID

### Service dependencies

The PRISM node is the only dependency of this service.
It is needed to verify information related to endorsements.

### Service API

We express a simplified service API as a grpc/protobuf definition for illustration purposes.
More RPCs will be added for convenience and state management. The API may have DID-based authentication, the authorized
DIDs are the ones contained in the service's set of trusted DIDs (see [service state](#service-state).

```proto
service EndorsementsService {
  // requests a fresh master key to be used, and associates its requester to the key
  // - Only keys without an associated DID will be returned
  // - No key will be returned twice 
  rpc getFreshMasterKey(GetFreshMasterKeyRequest) returns (GettFreshMasterKeyResponse) {}

  // requests to add en endorsement to a DID
  rpc endorseInstitution(EndorseInstitutionRequest) returns (EndorseInstitutionResponse) {}

  // requests information about the endorsements associated to a DID
  rpc getEndorsements(GetEndorsementsRequest) returns (GetEndorsementsResponse)

  // request to remove an existing endorsement
  rpc revokeEndorsement(RevokeEndorsementRequest) returns (RevokeEndorsementResponse) {}
}
```

### Service logic 

This section describes in more detail how to use the service API, and what each RPC does.
In order to illustrate the flows, let us imagine that we are the MoE, and we intend to onboard the first regional body.

1. First, we will request a fresh master key to the server using `getFreshMasterKey`
  - This RPC will receive our DID (the MoE DID), pick an un-used master key stored in the server along with its signature,
    and associate our DID to the returned key
  - We will verify that the returned key's signature is valid (this requires to resolve the MoE DID to retrieve the 
    signing key)
  - If valid, we will give this key to the regional body during the onboarding flow
2. The regional body will create a DID document in the following way:
   - It generates a `CreateDIDOperation` operation with a fresh master key
   - It also constructs a `DIDUpdateOperation` operation that adds the regional body issuing key, adds the key we provided
     as master key, and removes the original master key of this document from the list of valid keys
     NOTE: Additional **non-master** keys could be added as needed in this step
   - It then publishes in a single Cardano transaction the 2 operations in proper order by calling the `publishAsABlock`
     RPC in the node
   - Once the DID is resolvable by the node, it provides this DID to back to us
   - The result of this step is a published DID that has an issuing key controlled by the regional body, and its only
     master key is controlled by the MoE (us)
3. We now take the DID extended to us, we validate that the DID has as _only_ master key, the one we extended to the
   regional body
4. After validating the DID document ourselves, we create a verifiable credential using the SDK that represents our
   endorsement to the regional body. Note that we do not publish this VC yet. The VC contains the regional body's DID
   and has our DID as the issuer.
5. We call `endorseInstitution` providing the verifiable credential, and a signed `IssueBatchOperation` that only has 
   the provided VC as the merkle root, we also add the data related to the regional body (possibly an internal id 
   already present in the management system)
   - The service runs the following validations:
      + Extracts the endorsed DID from the VC and validates that the RPC caller is the proper endorser (i.e. the caller
        DID, is the DID associated to the key in the service internal state)
      + The endorser has rights to endorse the institution (i.e. in this case that the MoE can endorse a regional body)
      + The endorsed DID has as unique master key, one unused key associated to the endorser DID
      + The VC and signed operation are signed by the DID requester 
      + The merkle root of the signed operation matches the VC hash
   - If all checks pass, the issuance operation is sent to the node. The key, DID, and endorsement VC are associated to
     the endorsed institution in the service state. Finally, we add the newly endorsed DID to the set of trusted DIDs
6. Once endorsed, if we call `getEndorsements` for the endorsed DID, the service 
   will return a single element list, containing the VC and the validity period (which starts at the time when the 
   endorsement service processed the operation, note that this may differ from the time of the verifiable credential
   publication time in Cardano). If we call the RPC before the endorsement is dode, the list will return an empty list.
   Note that, if an institution is un-endorsed, and later endorsed back, the list will contain more elements indicating
   the VCs issued on each endorsement and their corresponding validity periods.
7. If for some reason we need to revoke the endorsement, we call `removeEndorsement` providing a signed
   `RevokeCredentialsOperation` that revokes the endorsement VC. The service forwards this operation to the node for
   to publish it, and removes the DID from the set of trusted DIDs. It also marks the timestamp that ends the validity
   of the most recent endorsemnt to the corresponding DID.

NOTE: It is probable that, after the revocation of an endorsement, the MoE may perform additional steps related to 
      updating the un-endorsed DID. This involves revoking the DID issuing key, and possibly revoking or re-issuing 
      credentials already issued by the revoked institution. See [key rotation scenario](#scenario-institution-needs-to-rotate-keys)
 
We want to guarantee the following invariants
- The service does not consider valid any endorsement to a DID that does not contain an authorized master key as the 
  only master key in the associated DID document
- No DID is endorsed if they do not have only one master key, which should be one of the authorised keys provided by the
  service
- All master keys of endorsed DIDs are controlled by the MoE
- No pair of endorse DIDs have a key (of any type) in common
    
## Onboarding of institutions

The onboarding of institutions will follow the same example flow described in [the service logic section](#service-logic)
The summary of the process is:
- An already trusted institution `A` wants to endorse institution `B`, so:
- `A` requests a fresh key `K` to the endorsements service
- `A` gives `K` to `B`
- `B` creates its DID, and update it to include `K` as its only master key.
  `B`'s DID could still have other non-master keys (e.g. an issuing key)
- `B` gives his published DID to `A`
- `A` resolves `B`'s DID, and validates that the only key master key of `B`'s DID document is indeed `K`
- `A` proceeds to create a verifiable credential that represents `A`'s endorsement to `B`'s DID, and extend this 
  credentials and corresponding signed atala operation to the endorsements service 
- The service makes adequate validations described in the previous section, and sends the operation to the node.
  The service also registers `B` DID as a trusted DID (an endorsed one)

For revoking an endorsement
- `A` would create a signed `RevokeCredentialsOperation` that revokes the endorsement credential originally issued to `B`
- `A` sends the credential to the endorsements service
- The endorsements service runs validations and forwards the operation to the node. 
- The service removes `B` from the set of trusted DIDs
- The MoE may require to perform further actions. See [key rotation scenario](#scenario-institution-needs-to-rotate-keys)

Note that the scenarios of:
- `B` key rotation
- `B`'s key, under the control of an attacker, issued/revoked credentials against `B`'s intention can be managed without
   requiring to revoke an endorsement.

Endorsement revocation should be an unusual operation that probably should only occur when a school/body/region will 
stop its role of issuing credentials. Endorsement revocation is not needed to recover from key losses or stolen issuing
keys. See the section below for more details.

## Scenario: institution needs to rotate keys

If the MoE needs to rotate a key, it has full autonomy to do so because it controls the master key of its own DID.

On the other hand, regional bodies, zones, and schools cannot update their DID documents because the MoE is the entity
that holds their master keys. In this situation, there will be a need to notify the MoE that a key needs to be rotated. 
The MoE will receive the information of:
- what key(s) needs to be revoked
- what key(s) needs to be added
and will send a corresponding signed `UpdateDIDOperation`. 

Key rotations are only needed when a key is lost, compromised, i.e. an attacker got access to the associated private 
key, or an attacker got access to the parent seed phrase. If this sensitive data is properly handled, the need for key
rotation should be rare.

The step to confirm the key rotation will likely require out-of-band confirmation for security reasons. E.g. a phone
call, or formal letter from a regional body administrator to authorize this action.

Now, there are other considerations to be aware of:
- If a key rotation is needed because an attacker is in control of the private key, then apart from key rotation, there
  may be a need to revoke un-authorized batches issued by the attacker.
- It is also possible for an attacker to revoke batches/credentials without authorization. This may require to re-issue
  the credentials.
- In order to detect the need for these additional actions (revoking/re-issuing batches), we recommend having an external
  administrative tool to explore the actions performed by a DID in a requested time period.
   + This could be built as a separate part of the system, or as a continuous auditing service. In any case, it is not
     part of the design scope of this document.

## UX flows

### Onboarding

This section is a draft proposal for the onbaording flow of institutions that need endorsements.

Today, the shared example flow to endorse an institution has been:
1. The region creates the school and sends and invite to the school leader
2. School leader finishes up registration (creates DID)
3. School and regions establish a connection
4. The region sends the endorsement credential to the school

The proposed change/addition is to get this process (institution 1 is the endorser and institution 2 is the endorsee):
1. Institution 1 invites institution 2
2. Institution 2, creates a DID and accepts connection
3. Institution 1 sends the key that institution 2 must use as master key (this key is controlled by the MoE)
4. Institution 2 updates its DID to only have that key as master key (along with an issuing key controlled by institution 2)
5. Institution 2 returns the DID to be endorsed (or informed that the DID has been properly updated)
6. Institution 1 verifies the DID (that it has the proper key as only master key)
7. Institution 1 issues verifiable credential and registers endorsement in the service

There could be a final step that shares the endrosement credentials to institution 2

If needed, the steps could be changed in order to share the master key to be used while stablishing the connection. The
endorsee could generate the DID and update it before accepting the connection. This merges 1 to 5 into just 2 steps. 

## External verification of students credentials

Our system relies on the assumption that there is a source of trust that provides verifiers the DIDs that correspond to
issuers. The endorsements service is a component that maintains the set of trusted DIDs in the deployment. For the internal
usage of the MoE network, the verification process could rely on querying the endorsements service. The service would 
provide information about a requested DID. In particular, it would inform in what time period has the DID been 
properly endorsed.

However, for verification outside the MoE deployment, i.e. for verifiers that do not have access to the endorsements 
service, we will need to export the information related to the endorsements to the external world. We could expose
an API from the endorsements service, or simply export the list of endorsed DIDs periodically.
Verifiers will later verify students' credentials using the node and SDK as usual. Additionally, they will validate that
the issuer DID is part of the exported list of trusted DIDs. Optionally, they could use the chain of endorsements' 
credentials provided by the student (or also exported by the MoE), and audit their validity according to the list. 

It is implicit that the MoE should expose publicly their DID for it to be used externally. Similarly, it could expose 
publicly the endorsement verifiable credentials, and institutions' DIDs.