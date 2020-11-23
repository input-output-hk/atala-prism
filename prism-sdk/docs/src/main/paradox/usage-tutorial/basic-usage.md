# Basic usage

In this section, you will find how to use the PRISM SDK for issuing simple credentials, all the operations done locally.

This assumes you already know how to setup an [sbt](https://www.scala-sbt.org/) project.

## Dependencies

First of all, add the necessary dependencies to your `build.sbt`:

```scala
libraryDependencies += "io.iohk" %% "prism-protos" % "@VERSION@" // needed for the credential payloads defined by protobuf
libraryDependencies += "io.iohk" %% "prism-crypto" % "@VERSION@" // needed to get a crypto implementation
libraryDependencies += "io.iohk" %% "prism-identity" % "@VERSION@" // needed to deal with DIDs
libraryDependencies += "io.iohk" %% "prism-credentials" % "@VERSION@" // needed to deal with credentials
```

## Imports
Import the PRISM modules:

```scala mdoc
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._
```

## Generate an identity
In order to use the PRISM ecosystem, you will need to generate an identity, PRISM provides [Decentralized Identifiers](https://w3c-ccg.github.io/did-spec/), for this section, we use an Unpublished DID. On such DID, the operations don't require network access, it is handy for trying the SDK, or, for use cases where you don't want others to discover such DID.


Generating an Unpublished DID is simple, we start generating a master key, taking the public key to generate the DID:

```scala mdoc
  val masterKeyPair = EC.generateKeyPair()
  val did = DID.createUnpublishedDID(masterKeyPair.publicKey)
```


Then, we need to get some details before being able to sign a credential, the [DID Document](https://w3c.github.io/did-core/#dfn-did-documents), and the id for the master key we used while creating our DID:

```scala mdoc
  // The DID Document is necessary to find the master key id, used to sign a credential
  val didDocument = did.asLongForm
    .flatMap(_.getInitialState.toOption)
    .flatMap(_.operation.createDid)
    .getOrElse(throw new RuntimeException("Impossible as we just created an unpublished DID"))

  // we have created the DID with a single public key
  val firstPublicKey = didDocument.didData
    .flatMap(_.publicKeys.headOption)
    .getOrElse(throw new RuntimeException("Impossible as we used a key to create the DID"))
```


## Generate a credential
Then, generating a credential requires defining the claims payload, which is a JSON, let's define a simple one:

```scala mdoc
  val credentialClaimsStr = """
                              |{
                              |  "name": "Jorge Lopez Portillo",
                              |  "degree": "Bachelor's in Self-Sovereign Identity Development"
                              |}""".stripMargin

  val credentialClaimsJson = io.circe.parser
    .parse(credentialClaimsStr)
    .getOrElse(throw new RuntimeException("Invalid json"))
```

The claims are what you are certifying in the credential, in this case, you certify that the given person holds the given bachelor's degree.

As you see, the claims do not include information about the issuer, to do so, we can proceed to create an `UnsignedCredential` object, where we take the claims, the issuer DID, and the key that needs to sign such credential: 


```scala mdoc
  val unsignedCredential = UnsignedCredentialBuilder[JsonBasedUnsignedCredential]
    .buildFrom(
      issuerDID = did,
      issuanceKeyId = firstPublicKey.id,
      claims = credentialClaimsJson
    )
```


At last, we can proceed to sign the actual credential:

```scala mdoc
  val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, masterKeyPair.privateKey)(EC)
```

That's it, until know you were able to create and sign a credential, on the next steps, you will learn how to get it published to the Cardano network.
