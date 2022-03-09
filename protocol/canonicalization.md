<!-- This is meant to be part of a larger document -->

\newpage

# Canonicalization (and comments on signing)

Our protocol uses cryptographic signatures and hash functions to guarantee its security (along with other properties). 
The use of these cryptographic primitives requires to translate programming languages data representations into 
sequences of bytes. This process receives the name of _data serialization_. Once data is serialized, we can hash and/or 
sign it.

In this document, we will describe what we are doing and we will explore some challenges related to signing data and 
exchanging it between many applications. At the of the document, we will describe a rudimentary signing technique that
was implemented as a proof of concept for credentials. We will also comment about a more robust implementation and 
future challenges that may come.

## Our current state

There are mainly three places where we are hashing/signing data.

- During Atala Operations construction
- When we compute a DID suffix, where we hash the initial DIDData
- When we need to sign credentials

Let's explore the approach we have on the three situations.

### Signing atala operations

The way in which we are generating the bytes to sign from Atala Operations is through protobuf messages. We have:

```
message AtalaOperation {
    oneof operation {
        CreateDIDOperation createDid = 1;
        UpdateDIDOperation updateDid = 2;
        IssueCredentialOperation issueCredential = 3;
        RevokeCredentialOperation revokeCredential = 4;
    };
}

message SignedAtalaOperation {
    string signedWith = 1; // id of key used to sign
    bytes signature = 2; // signature of byte encoding of the operation
    AtalaOperation operation = 3;
}
```

In order to construct a `SignedAtalaOperation`:

1. We construct the needed `AtalaOperation` message using the corresponding protobuf model
2. We extract the bytes produced by protobuf based on the model
3. We sign those bytes and we build a `SignedAtalaOperation` message that is then serialized and posted as part of 
   transaction metadata.

Nodes later validate the signature in the following way:

1. They see the message in transactions metadata 
2. They decode the `SignedAtalaOperation` and extract the key, signature and the operation that was theoretically signed
3. They serialize again the `AtalaOperation` into a sequence of bytes and check the signature against those bytes
   
The process works independently of the programming language and platform used to generate the signature and the one used
to verify it because protobuf is _currently_ providing the same bytes from our messages in all platforms. This means 
that protobuf is _currently_ providing a _canonical_ bytes representation of the serialized data.

However, we must remark that this is not a feature that protobuf guarantees nor provides in all situations. For example,
if our models use [maps](https://developers.google.com/protocol-buffers/docs/proto3#maps) then, the "canonical bytes" 
property we rely on would be lost, because different languages may encode maps in different ways. Furthermore, protobuf
[specification](https://developers.google.com/protocol-buffers/docs/encoding#implications) advise to not assume the byte
output of a serialized message is stable.

If the application that creates the `AtalaOperation` would generate different bytes as serialization than the ones the 
node generates when serializing the operation, then the signature validation process would fail (because the bytes 
signed by the first application would not match the bytes used by the node during verification). This could for example
happen if the verifying party is using old versions of the protobuf models.

In order to solve these issues, we should consider to attach the signed bytes "as is" in the encoded protobuf messages.
For example, for `SignedAtalaOperation` we should refactor the message to:

```
message SignedAtalaOperation {
    string signedWith = 1; // id of key used to sign
    bytes signature = 2; // signature of byte encoding of the operation
    bytes operation = 3;
}
```

### Computing DID suffix

For the computation of the DID suffix of a given initial state of a DID Document, we face a similar situation as the one
before. Our protocol defines that the DID suffix associated to a DID Document is the hash of certain DID Data associated
with the initial state of the document. An important property we have is that clients are able to compute a DID suffix 
without the need of publishing it.

***NOTE***: We are currently hashing the entire `AtalaOperation` (that contains a `CreateDIDOperation`) and not the 
`DIDData` part.

The way in which we achieve consistent hashes in the client and node side is that given the models associated to these 
protobuf messages:

```
enum KeyUsage {
    // UNKNOWN_KEY is an invalid value - Protobuf uses 0 if no value is provided and we want user to explicitly choose the usage
    UNKNOWN_KEY = 0;
    MASTER_KEY = 1;
    ISSUING_KEY = 2;
    COMMUNICATION_KEY = 3;
    AUTHENTICATION_KEY = 4;
}

message ECKeyData {
    string curve = 1;
    bytes x = 2;
    bytes y = 3;
}

message PublicKey {
    string id = 1;
    KeyUsage usage = 2;
    oneof keyData {
        ECKeyData ecKeyData = 8;
    };
}

message DIDData {
    string id = 1; // DID suffix, where DID is in form did:atala:[DID suffix]
    repeated PublicKey publicKeys = 2;
}

message CreateDIDOperation {
    DIDData didData = 1; // DIDData with did empty id field
}
```

both (client and node) can construct the DID suffix by hashing the bytes of the corresponding `AtalaOperation`
message. Note again, that this still depends on the weak assumption that we can trust on the stability of the bytes
obtained.

### Credential signing

There is currently a PoC that needs to be reviewed on this topic. So we won't expand about any approach for now.
We will document the final approach in this section. At the end of this document, there are some comments on a simple
approach.

## Comments on JSON

There has been conversation related to the use of JSON to model credentials. It is also the case that both
[DID Core](https://www.w3.org/TR/did-core/) and [Verifiable Credentials Data Model](https://www.w3.org/TR/vc-data-model/#syntaxes)
drafts provide JSON and JSON-LD based descriptions of their data models.

According to [ECMA-404](http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf), 
JSON is a text syntax that facilitates structured data interchange between all programming languages. However, on the 
same document it is stated that 

> The JSON syntax is not a specification of a complete data interchange. Meaningful data interchange requires agreement
> between a producer and consumer on the semantics attached to a particular use of the JSON syntax. What JSON does
> provide is the syntactic framework to which such semantics can be attached.

JSON's simplicity favoured its wide adoption. However, this adoption came with some interoperability problems. 

[ECMA-404](http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf) says:

> The JSON syntax does not impose any restrictions on the strings used as names, does not require that name strings be
> unique, and does not assign any significance to the ordering of name/value pairs. These are all semantic 
> considerations that may be defined by JSON processors or in specifications defining specific uses of JSON for data
> interchange. 

On [JSON RFC 8259](https://www.rfc-editor.org/rfc/rfc8259.txt) we see statements like:

> The names within an object SHOULD be unique.

Meaning that names could be repeated according to [RFC 8174](https://tools.ietf.org/html/rfc8174) definition of "SHOULD"

The RFC mentions differences on implementations based on repeated fields:

> An object whose names are all unique is interoperable in the sense that all software implementations receiving that
> object will agree on the name-value mappings. When the names within an object are not unique, the behavior of software
> that receives such an object is unpredictable. Many implementations report the last name/value pair only. Other
> implementations report an error or fail to parse the object, and some implementations report all of the name/value 
> pairs, including duplicates.
  
On the topic of element ordering the same RFC says:

> JSON parsing libraries have been observed to differ as to whether or not they make the ordering of object members
> visible to calling software. Implementations whose behavior does not depend on member ordering will be interoperable
> in the sense that they will not be affected by these differences.

which is a relevant point to obtain canonicalization if one needs to hash/sign the same JSON in multiple applications.

The RFC and ECMA are consistent with respect to the definition of JSON texts. However, ECMA-404 allows several practices
that the RFC specification recommends avoiding in the interests of maximal interoperability.

[DID Core](https://www.w3.org/TR/did-core/) draft used to define DID Documents **as** JSONs. At the time of this writing
the specification has no text in the [Data model section](https://www.w3.org/TR/did-core/#data-model) and does not
define a DID Document as a JSON anymore. It does talk about JSON, JSON-LD and CBOR core representations.

On section 8 ([Core representations](https://www.w3.org/TR/did-core/#core-representations)) it says:

> All concrete representations of a DID document MUST be serialized using a deterministic mapping that is able to be 
> unambiguously parsed into the data model defined in this specification. All serialization methods MUST define rules 
> for the bidirectional translation of a DID document both into and out of the representation in question. As a 
> consequence, translation between any two representations MUST be done by parsing the source format into a DID document
> model (described in Sections § 6. Data Model and § 3.3 DID Documents) and then serializing the DID document model into
> the target representation. An implementation MUST NOT convert between representations without first parsing to a DID
> document model.
  
The lack of a canonical binary representation of JSON texts makes them not ideal for cryptographic treatment. There are 
different proposals to get canonical JSON serialization, none of which seems to be considered a formal standard.

### JCS

[JCS (IETF Draft 17)](https://tools.ietf.org/html/draft-rundgren-json-canonicalization-scheme-17) is a canonization 
proposal for JSON texts. The JCS specification defines how to create a canonical representation of JSON data by building
on the strict serialization methods for JSON primitives defined by ECMAScript, constraining JSON data to the I-JSON 
[RFC7493](https://www.rfc-editor.org/rfc/rfc7493.html) subset, and by using deterministic property sorting. 

We found [implementations](https://github.com/cyberphone/json-canonicalization) in different languages, including Java.
There is also an available [JWS-JCS PoC](https://github.com/cyberphone/jws-jcs).

## On JSON-LD, RDF and LD-PROOFS

Another alternative proposed so far is the use of Linked Data structures and LD-PROOFS.

[LD-PROOFS](https://w3c-ccg.github.io/ld-proofs/) is an experimental specification on how to sign Linked Data. At the 
time of this writing, the work published doesn't seem robust. The draft is from March 2020 and says:

> This specification was published by the W3C Digital Verification Community Group. It is not a W3C Standard nor is it 
> on the W3C Standards Track. Please note that under the W3C Community Contributor License Agreement (CLA) there is a
> limited opt-out and other conditions apply. Learn more about W3C Community and Business Groups.
  
>  This is an experimental specification and is undergoing regular revisions. It is not fit for production deployment.
  
The document doesn't clearly define any proof type or explain how to use them. Furthermore, on the section titled
[Creating New Proof Types](https://w3c-ccg.github.io/ld-proofs/#creating-new-proof-types), we infer that the
specification plans to describe with Linked Data representation (illustrated with [JSON-LD](https://www.w3.org/TR/json-ld/)
properties like `canonicalizationAlgorithm`, which would refer to the canonicalization algorithm used to produce the 
proof. Hence, this does not seem to focus on defining a canonical representation. It instead attempts to give a way to
inform all needed data to verify a generated proof.
  
[JSON-LD](https://www.w3.org/TR/json-ld/) is a specific JSON based format to serialise Linked Data. At the time of this
writing, its status is a Candidate Recommendation and is believed to soon become an endorsed recommendation from W3C.
In particular, 

> JSON-LD is a concrete RDF syntax as described in [RDF11-CONCEPTS](https://www.w3.org/TR/rdf11-concepts/). 
> Hence, a JSON-LD document is both an RDF document and a JSON document and correspondingly represents an instance of 
> an RDF data model. However, JSON-LD also extends the RDF data model...
>
> Summarized, these differences mean that JSON-LD is capable of serializing any RDF graph or dataset and most, but not 
> all, JSON-LD documents can be directly interpreted as RDF as described in RDF 1.1 Concepts.

[RDF](https://www.w3.org/TR/rdf11-concepts/) is another syntax used to describe resources and linked data.
In particular, RDF is an official W3C recommendation. 

[RDF-JSON](https://www.w3.org/TR/rdf-json/) was an initiative to represent JSON documents with RDF. However, we can see
the following message in the draft page:

> The RDF Working Group has decided not to push this document through the W3C Recommendation Track. You should therefore
> not expect to see this document eventually become a W3C Recommendation.
> This document was published as a Working Group Note to provide those who are using it and/or have an interest in it 
> with a stable reference.
> The RDF Working Group decided to put JSON-LD on the Recommendation track. Therefore, unless you have a specific reason
> to use the syntax defined in this document instead of JSON-LD, you are encouraged to use JSON-LD.

We didn't invest further time researching on Linked Data signatures after exploring LD-PROOFs. If needed, we could ask
for an update in DIF slack.

## Comments on credentials' signing

Research aside, a simple process we could follow to sign credentials is:

1. Model the credential data as a JSON due to its flexibility
2. Serialize the JSON to an array of bytes
3. Sign the bytes
4. Define our `GenericCredential` as based64url of the serialized bytes of the following JSON
 
```
{
  signature:  base64url(signature),
  credential: base64url(bytes(credential_data))
}
```

The recipient will have to store the bytes "as is" to preserve a canonical hashable representation.

Alternatively, instead of using JSON we could create the following protobuf message

``` 
message GenericCredential {
  bytes signature = 1;
  bytes credential = 2;  
}
```

In both cases, the signing key reference can live inside the credential.
The use of protobuf, as an alternative to JSON, allows to obtain the bytes of the `GenericCredential` and hash them to 
post them in the blockchain. Later, if the credential is shared to a client using another implementation language, we 
wouldn't need to worry about canonicalization to compute the hash as long as the protobuf message is exchanged. This is 
the same trick we are using for operation signing and DID suffix computation.

An even simpler approach was implemented as a 
[proof of concept](https://github.com/input-output-hk/atala/commit/03999acb5e2a1c6da461b0db89d94c6183d52c71).
In that PoC, the generic credential was represented as a pair of dot (.) separated strings, where the first string
represented the encoded bytes of the credential signed and the second string represented the encoded bytes of the
signature.

```
 base64url(bytes(credential)).base64url(signature)
```

This model resembles to [JWS](https://tools.ietf.org/html/rfc7515). The main difference is that JWS contains a header
with key reference, signing algorithm information and other data. We are planning to implement a JWS based version of
verifiable credentials for MVP scope.

## A note on future goals (selective disclosure)

We want to remark that, in order to implement selective disclosure, we may probably need something different to the
approach described in the previous section.
For example, if we use a Merkle Tree based approach, the bytes to sign are the ones corresponding to the merkle root of
the eventual generic credential tree and not the credential bytes themselves. This implies that the credential to share
could be of the form:

```
{
  proof: base64url( bytes(  {
    signature:  base64url(signature),
    root: base64url(merkle_root)
  } ) ),
  credential: Actual_credential_data
}
```

and we would need a canonical transformation from the credential data to the Merkle Tree used to compute the root. The 
hash of the `proof` property would be posted as a witness on-chain.
Later, on selective disclosure, a user would send the following data:

```
{
  witness: base64url( hash(the `proof` property defined above) ),
  data_revealed: [
    {
      value: value_revealed,
      nonce: a nonce associated to the revealed value, 
      path: [ ... ], // basicaly a list of left-right indicators
      hashes: [ ...] // list of base64url hashes that need to be appended following `path` instructions to recompute the
                     // merkle root 
    },
    ...
  ]
}
```

This may also be changed if we move toward ZK proofs.
