# Endorsements

## Context and problem statement

The Ministry of Education (MoE) would like to represent endorsements
between institutions to display which entities can issue credentials. 
Given the big number of schools to be endorsed, the MoE would like to
delegate some management responsibility to other administrative institutions:
- The MoE would endorse regional bodies first,
- The regional bodies would endorse zones
- The zones would endorse schools
- The schools do not endorse other entities, but they issue credentials
  to students

There are two core points to refine here
1. How do we represent adding an endorsement, and how do we represent
   the removal of an endorsement. Aa a colorary to this, we need
   to define the logic of what (type of) entities could endorse others 
2. Once an endorsement is removed, how do we maintain control over
   credentials and endorsements already issued by the un-endorsed
   institution. E.g. if a zone removes the endorsement of a school, we 
   maintain previously issued credentials by the school as valid. 
   However, what if we eventually need to revoke one of those
   credentials? Similarly, if a zone endorsement is removed by its
   regional body, how could we remove endorsements granted by the zone
   to schools (when/if needed)?

Regarding point 1, we should define what lives at application level, 
what lives at protocol level, what is accessible from the internal MoE
network, and what is accessible from the outside world.

We could manage the list of endorsed institutions and who endorsed them
at application level. This could be a simple service that just keeps a
list of endorsement relations and timestamps. The _endorsements service_
could also control, at application level, which endorsers have rights to
endorse other entities. The interface for this service could simply return
the endorsement chain and timestamps for a given institution/DID.

At protocol level, we do not have primitives to represent logic related
to endorsements. As an intuitive idea, we could issue verifiable credentials
stating these endorsements. However, we have found problems with this approach.
We will describe the problem while discussing point 2 (i.e. endorsements/credentials
revocation). The intuitive reason behind the problem is that, even though any
entity could have control upon their _direct_ endorsements, no entity has power
to enforce rules about other endorsements down the road.

As a consequence, we think that credentials could be used for auditing
purposes, but the source of trust for the chain of endorsements should
remain at application level. 

With respect to visibility inside and outside the MoE network, we will propose
for the MoE system to depend on the endorsements service to validate chains of
trust. The external world could either also depend on the MoE endorsements
service, or on other service to provide a trusted list of DIDs (possibly with
an attachment of endorsement relations and/or associated verifiable credentials).


Now, regarding the second point, i.e. how to revoke credentials/endorsements
issued by a newly un-endorsed institution, we could also think at both application,
and protocol level. 
The endorsements service could be updated by a simple authority check to revoke 
endorsements. However, this does not create a revocation of endorsements at the 
level of verifiable credentials.

To illustrate this, imagine the following scenario. The MoE is able to issue and 
revoke endorsement credentials upon regions, but the Ministry does not have control to 
issue/revoke endorsements upon the schools that are endorsed by a zone that has been 
endorsed by a region, this is because only the issuer of a credential can revoke it (and
the ministry is not issuing the credentials that regional bodies nor zones create). It is
true that, in the ideal scenario, all intermediate actors (i.e. regions and zones) would
cooperate towards the endorsing of authorized schools.
However, from a security analysis, if a key controlled by a region falls under control 
of an attacker, the attacker could endorse a "fake zone" and, through that artificial 
zone, it could endorse a fake school, which could later issue credentials to fake 
students. The region could detect and revoke the endorsement issued by the attacker to
the zone (the direct endorsement made with the region key) but, the region would not be
able to revoke the zone endorsement to the fake school, nor the credentials issued to
fake students. This example scenario leaves us with a chain of verifiable credentials 
that satisfy a valid chain of endorsements but represent an endorsement to a fake school.

The above describes the reason why we suggest the use of the endorsements service at 
application level, instead of using verifiable credentials to represent chains of trust.

Now, the problem of revoking credentials when an attacker controls a key
is something we need to solve at protocol level not just for endorsements,
but also for the credentials that are issued to students. 
For this, we have analyzed the alternatives discribed in the next sections.
 
### All institutions share the same DID

This means that the MoE creates its DID, and all regional bodies, zones and 
schools will have an issuance key in the MoE DID document. The only master key
would be controlled by the MoE.

Positive points
- It gives us a clear on-chain representation of endorsements
  (issuance key presence in the MoE DID Document)
- It allows the MoE to revoke anything if needed
- Requires no protocol change

Negative points
- Any regional body, zone and school can revoke credentials at protocol level
  that may have been issued by other institutions. The event would be detectable,
  the MoE could revoke the key of the institution with the misbehavior, and
  later, institutions could re-issue needed credentials (implementing all these
  mechanisms for auditing and detecting issues would require extra work).
- The MoE needs to send operations to add each key. The process could batch
  keys, and the signatures could be periodic. But the MoE will have to perform
  actions to enable this.
- The issuer DID in credentials would not be enough to infer which institution
  issued it. We could mitigate this by adding the institution name in the 
  issued credential, and use descriptive key ids associated to the
  institution's key.

### Multiple DIDs, all have the MoE master key
 
In this scenario, each institution has its own DID, but their DID documents
will contain only one master key each. The master key can be different on 
each document, but all are controlled by the MoD.

Positive
- With a minor protocol change, the MoE does not require to sign every DID
  creation. Hence, it could delegate more actions compared to the previous 
  approach.
- Institutions (regions, zones, schools) cannot revoke credentials not issued 
  by them.

Negative  
- Less explicit representation for endorsements at protocol level
- Requires minor protocol change (in order to remove a master key without 
  needing the signature of the last remaining key)
- Only MoE can revoke other's credentials. So, no perfect delegation of duties.
- The MoE need to sign DID updates of all the other DIDs. This is, whenever
  a key is lost/compromised on a lower level institution, the MoE is requires
  to intervine in order to rotate the issuance keys on behalf of the attacked
  institution.

#### Small variation - recovery keys

We could change the protocol and add a "recovery key" along with a "recover"
operation. Such operation would revoke all keys in a DID document except for
a new master key added by this operation. Such recovery key could be different
on each institution DID document, but all of the recovery keys could be in
control of the MoE. We could then leave master keys under the control of 
institutions.

The idea of this type of key comes from Sidetree. This is an instance of the
notion of key hierarchies (we are basically just adding a key level on top
of Master Key). 
 
Positive
- This could lead to less work for key updates on the MoE side. 
  Updating an issuance key in an institution DID Document would be doable without
  the MoE intervention.
- This key could be stored in "colder" storage than master keys, as their use 
  is intended for emergencies or more limited set of updates. 
 
Negative
- If lower level institutions' master keys are compromised, the MoE needs to 
  intervene. So, we only fix key rotation for issuance key, but not for master
  keys

### Key derivation-like scheme

We also considered the idea that the MoE could start with a master key, and
derive somehow the keys for regional bodies DIDs, which would later derive
keys for zones, and finally these could derive keys for schools. The idea 
would be that entities up in the hierarchy could always derive needed keys to
take control of lower level entities.

The problem we see for this approach, is that the protocol does not enforce the
use of any key after initialization of this setting. Meaning, even if we
could enforce the initial use of a correctly derived key, the institution that
wants to behave wrongly (or an attacker controlling such keys) could always
rotate keys to use any key of its desire while revoking the key that could be
derived from other authorities

Positive
- It may not require protocol changes

Negative
- We do not see a way to enforce the use of desired keys

### Controller property from DID Core

There is a [property defined in DID Core](https://www.w3.org/TR/did-core/#did-controller)
called `controller`, which seems to represent the semantic behaviour we are
looking for.

> A DID controller is an entity that is authorized to make changes to a DID 
> document. The process of authorizing a DID controller is defined by the DID
> method. 
 
Note: in the text below, the term `verification method` is a concept equivalent to
`public key` in our terminology (the spec and chats hereon use that term).  
 
After some research interacting in DIF slack/calls, reading the spec, and
some github issues pointed by DIF members, we concluded that:
- The `controller` property is used at two different levels in a DID document (with different meaning in each place).
  One place is the top-level of the document, the other place is inside a verification method object (a JSON describing
  a public key to put it in simpler terms)
- There is no reference on how top-level `controller` property should be used.
  + In particular, nobody could point us to a DID method making use of this 
    property
- There seems to be no agreement on how to use the verifition method level 
  `controller` property in the scenario where the controller does not match
  the `id` of the DID document itself. There are interoperability related discussions
  that suggest that this controller property should also be the value in top-level
  `controller` which should also match the DID document id. This is
  
  ```
   did_document.id === did_document.verificationMethod[N].controller === did_document.controller 
  ```
  
Leaving aside the above feedback, the way in which we thought we could use the `controller` property at top-level of the
DID document as follows.
- First, update our protocol with a logic on how to add/remove/manage controllers to a DID. This is not exactly trivial,
  consider cases of multiple controllers for a single DID, or the need to avoid a controller to add others.
  See the concrete explanation at the end of this document with corresponding simplified proposal
- Then, update operations' logic to allow DID updates signed by controller DIDs

Having those protocol updates, we could construct this setting
- Each institution will have their own DID
- Regional bodies would have the MoE DID as their controller
- Zones DIDs would have their corresponding regional body as controller
- Schools would have their corresponding zone as controller

Given those changes and the initial setting described for DIDs we could ilustrate how to act with the following example:
If a zone needs to revoke a VC issued by a school:
- The zone would use its DID to revoke the keys of the school's DID document, 
  and add keys controlled by itself
- The zone could now use the new keys to revoke credentials issued by the school

Similarly, imagine a case when a zone lost it's keys and needs to revoke a VC
issued by a school. 
- The proper regional body uses its DID to update the zone's DID, revoking its keys
  and adding new ones
- Now, the keys in the zone updated DID document could be used to take control of the
  desired school DID
- Then the VC could be revoked using the new keys in the school DID document

Transitively, we could continue the path on up to the MoE being able to
take control of any DID.

Positive
- It allows full delegation of actions. Hence, any issue could be managed
  by the lowest entity in the hierarchy

Negative
- It requires non-trivial changes to the protocol
  + E.g. note that for the approach described above to work,
    we need a logic that does not allow a controller DID
    to remove itself from the list of controllers. If that 
    would be possible, an attacker controlling a zone and 
    school related keys, could remove it's corresponding
    zone DID from the school controllers list, and the school
    could scape from the hierarchy of possible updates.
    Less complex scenarios would have similar effects if a 
    master key has the power to change its own DID
    document's controllers.
    Above notes aside, we may be able to start with a logic of
    "for any DID, it can only have one controller on its lifespan"
    This relates to this [GH issue in DID core](https://github.com/w3c/did-core/issues/719)
- It may add complexity to off-line requirement, and the
  management of local/temporal/unconfirmed state
- It may bring incompatibility issues with DID core and VC standards according
  to a chat conversation we had (see references, in particular the chat in
  Sidetree's channel)
 

# Conclusion

In order to provide full delegation for action (i.e. ability to issue/revoke verifiable credentials, and manage DID 
document's own keys), we seem to need something akin the top-level controller property described in DID Core.
A priori, we do not have proposals to mitigate the warnings about future interoperability issues. Note that, in the 
future, a top-level controller could add a new verification method that has the controller DID as verification method
controller (violating the suggestion of keeping 

`did_document.id === did_document.verificationMethod[N].controller === did_document.controller`

(see references at the end of the document)

The closest alternatives are to:
- either change the protocol to have recovery keys controlled by the MoE. This forces the MoE to
  act when there is a need to update institutions' compromised master keys, or
- have for every DID a single master key controlled by the MoE. This forces the MoE to
  act when there is a need to update institutions' compromised keys (i.e. not just the master key case)


# References

## GH issues

Some GH issues explored (the list could be extended, but we opted to limit the time spent on this)
- [GH issue: FAQ Question: Can the DID Controller of a DID Document be updated/changed after the DID Document has been
  registered? #719](https://github.com/w3c/did-core/issues/719)
- [GH issue; Unclear which verification methods are authorized for did document operations #195](https://github.com/w3c/did-core/issues/195)
- [GH issue: transfer of controllership and it's intersection with the subject of an identifier #269](https://github.com/w3c/did-core/issues/269)
- [GH issue: When is a DID subject not a DID controller (if ever)? #122](https://github.com/w3c/did-core/issues/122)
- [GH issue: Potential problems with requiring "controller" for "verificationMethod" #697](https://github.com/w3c/did-core/issues/697)

## DIF slack (extracted from chats)

### From Identities Working Group (wg-id channel)

> Friday 7th, May 2021 
> 
> ezequiel  4:04 PM 
> 
> my apologies in advance if this is the wrong channel (please point me to a better one if needed, and I will move the 
> message there)
> I am reading the spec and GH issues, and I am a bit confused on how to use/interpret the controller property both at
> top level and in verification methods. My understanding currently is that:
> - the top-level controller (if present), denotes single DID or set of DIDs that have power to update the DID Document
>   that holds this property. The logic/rules of how these DIDs can update the DID document are defined by each DID
>   Method. Is this correct? Is there any general imposition on such rules by DID Core?
> - Now, the verification method controller confuses me a bit more. In this case, it is always a single DID. I understand
>   that this indicates which entity is controlling this verification method. However, does this (controller) DID have
>   any play at the time of using the verification method? The main part that confuses me, is how this DID act apart 
>   from being an indication of the controller
> is there a document/section (that I could very well have missed) with examples, or a more detailed explanation on the
> use of this property in its different locations?
> As always, thank you in advance for the help
>
> Markus Sabadello (Danube Tech)  Monday 10th, May 2021
> 
> Hello @ezequiel, this is definitely the right channel for discussing topics related to DID Core. The controller 
> property is probably one of the most difficult parts to understand about DID Core. One of the places where it was
> discussed is https://github.com/w3c/did-core/issues/697. Do you maybe want to join today's ID WG call to talk about
> this? https://github.com/decentralized-identity/identifiers-discovery/blob/main/agenda.md.
>
> Markus Sabadello (Danube Tech)
> 
> I'll try to give a quick answer to your questions here:
> 1. Regarding "controller" on the top level, I think the reality is that this isn't 100% well defined. As you say,
>    the expectation is that DID methods use this property to allow controllers of other DIDs to update the DID document
>    that has the "controller" property. But at the same, DID methods can also use other mechanisms (that don't use the
>    DID document) to determine who can update the DID document.
> 2. Regarding "controller" on a verification method: I agree this is even more confusing, and maybe the property is
>    mis-named. My own understanding is that the DID listed as "controller" is NOT used when using the verification
>    method. It is only used by linked data processors to link the verification methods to their DID documents in a
>    bi-directional way.
>
> ezequiel
> 
> thank you for the responses @Markus Sabadello (Danube Tech)
>  > One of the places where it was discussed is https://github.com/w3c/did-core/issues/697.
> I actually started on that github issue after reading the spec, which helped but didn't clarify all my doubts
>
>  > Do you maybe want to join today's ID WG call to talk about this?
> I will try to make it, but I wouldn't like to disrupt the call with introductory questions either
> With respect to point 1, I think I understand the same, i.e. DID methods can ignore this property and use other
> custom mechanism. I am researching the property for a use case I have
> With respect to point 2, I am not understanding the bi-directional connection. Does this mean that the DID
> document associated to the DID in the controller value of a verification method should make some reference to the
> verification method too? Here is where an example of how the verification method's DID document and the controller
> associated DID document may be helpful for my current lack of understanding

At the time of these writings, the thread ended there. We joined the ID WG call but the topic of this PR was not covered,
it was pointed for the next call (Monday 17th)

### From sidetree slack (wg-sidetree)

> May 7th 2021
> 
> ezequiel
> 
> Hello, I have a question. I see in the spec the use of verification method maps with corresponding controller property
> However, does Sidetree define rules to add/remove top level controllers, i.e. at he top level map of the DID document?
> If not, is there a recommended way to implement the functionality where a controller would like to update a DID 
> document on behalf of the document's subject?

We re-asked on Monday after receiving no initial attention.

*we got this responses in a thread (which looks relevant for future standard compatibility)*
> Orie Steele (Transmute) 
> 
> sidetree does not expose a controller API
> all sidetree did documents assume the DID Document ID is the controller
> this limits some complex functionality and can cause interoperability issues with proofs related to VCs where did
> document.id might not match did document.key.controller
> implementers are advised to always make controller === did document . id
> or should be advised…

In the main channel we also received these answers
> daniel  3:37 PM
> 
> @ezequiel this isn't a feature that was supported in this version of the spec
> Could look to do so in a revision, if need be
> 
> ezequiel  3:45 PM
> 
> I see, thank you @daniel
> I kinda stepped upon a scenario where controller looks as an interesting solution, and could not make my mind around
> how to solve it using Sidetree as is today (or as I recall it from my last visit to the spec to be more precise)
> It relates to hierarchical authorization, and the ability for a high-level authority to revoke lower-level 
> authorities' actions
>  
> daniel  3:46 PM
> 
> Why would the lower-level authority not just have a verifiable credential issued to them by the higher-level 
> authority, which the authority could revoke at any time? I feel like it is an anti-pattern to mix cross-entity 
> DID relationships into the DID Doc like that, personally
> 
> Orie Steele (Transmute)  3:49 PM
> 
> yeah, ^ that is a fair point
> this is one of those places where sidetree is more opinionated that did core
> and it might be a security feature.
>  
> ezequiel  3:50 PM
> 
> maybe I can try to explain the pattern to see how bad this is used
> Entity A authorizes entities B1, B2, B3 to perform actions on its behalf
> Entity B1 authorizes entities C1 and C2 to perform actions on its behalf
> Entities C1 and C2 issue VCs on behalf of A
> Later, B1 revokes C1 rights to issue credentials on behalf of A
> Now B1 would like to revoke credentials issued by C1, but the revocation repository requires issuer key (C1) to revoke C1's issued credentials
>  
> Orie Steele (Transmute)  3:51 PM
> 
> https://w3c.github.io/did-core/#capability-delegation
> https://w3c.github.io/did-core/#capability-invocation
> vs
> https://w3c.github.io/did-core/#assertion
> the capability to issue (edited) 
> and the key used to issue should be different
> but they could be the same
>  
> ezequiel  3:53 PM
> 
> yes, I came across those links but that left me towards questions regarding how the controller property works both at
> top level and at verification method level
>
> Orie Steele (Transmute)  3:53 PM
> 
> ahh the top level controller
> is different than the controller in a key
> top level controller is not supported by sidetree.
> at least today.
> its unclear what its use case really is
>  
> ezequiel  3:55 PM
> 
> I found it useful for transitively take control of other DIDs in the hierarchy mentioned above
> so, A was controller of B1, B2 and B3, then B1 of C1 and C2
> In that way, anyone could fully delegate operation to each other, and if anything bad happened, A could control B1, 
> and from there control C1 and then revoke C1 issued credentials
> all through key updates
> I read the verification method level controller. But I got stuck because I could not follow how it should be used
> 
> Orie Steele (Transmute)  4:00 PM
> 
> from an interop standpoint
> did_document.id === did_document.verificationMethod[N].controller === did_document.controller (edited) 
> is always safe
> anything else is not guaranteed to work / not cause problems.
>
> ezequiel  4:05 PM
>
> > did_document.id === did_document.verificationMethod[N].controller === did_document.controller (edited)
> I didn't follow this, sorry @Orie Steele (Transmute), would you mind to expand?
> Is this saying that all verification method controllers of a document should be equal to the id of the document, 
> which should be one of the DID controllers of the top-level doc?
> 
> btw, the issue/problem I found about using verification method level controller was that the DID document controller (C1 in my example) is always free to remove that verification method from its document, leaving B1 unable to later revoke anything that C1 issued before his rights were revoked
> this makes me believe that I am not understanding how the verification method level controller should be used
>
> Orie Steele (Transmute)  5:38 PM
> 
> I’m saying this is safe:

```json
   {
      "@context": [
        "https://www.w3.org/ns/did/v1", "https://ns.did.ai/suites/x25519-2018/v1",
      ],
      "id": "did:key:z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc",
      "controller": "did:key:z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc",
      "verificationMethod": [
        {
          "id": "did:key:z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc#z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc",
          "type": "JsonWebKey2020",
          "controller": "did:key:z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc",
          "publicKeyJwk": {
            "kty": "OKP",
            "crv": "X25519",
            "x": "5SSNqMApS2KzarctGJbSPlnjH4CmY7eDPQ9w_dbU7SE"
          }
        }
     ],
     "keyAgreement": [
        "did:key:z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc#z6LSs6erb2Y5uNhMSJ4vkXdpSwAwZW2EYoV62HRi26cFgjCc"
      ]
   }
```
> and any other values for controller
> are unsafe.
> mostly because there lacks clear description in a spec about them
> including in did core.

We also attended the sidetree WG call (of Tuesday 11th) and expose our scenario with no 
suggestions on how to implement such behavior using Sidetree nor the controller properties