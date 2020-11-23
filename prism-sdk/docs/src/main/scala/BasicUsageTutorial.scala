import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._

/**
  * This reflects the snippets that are in the markdown docs, allowing us to get autocompletion
  * while editing them
  *
  * See /src/main/paradox/usage-tutorial/basic-usage.md
  */
object BasicUsageTutorial {
  val masterKeyPair = EC.generateKeyPair()
  val did = DID.createUnpublishedDID(masterKeyPair.publicKey)

  // The DID Document is necessary to find the master key id, used to sign a credential
  val didDocument = did.asLongForm
    .flatMap(_.getInitialState.toOption)
    .flatMap(_.operation.createDid)
    .getOrElse(throw new RuntimeException("Impossible as we just created an unpublished DID"))

  // we have created the DID with a single public key
  val firstPublicKey = didDocument.didData
    .flatMap(_.publicKeys.headOption)
    .getOrElse(throw new RuntimeException("Impossible as we used a key to create the DID"))

  val credentialClaimsStr = """
                              |{
                              |  "name": "Jorge Lopez Portillo",
                              |  "degree": "Bachelor's in Self-Sovereign Identity Development"
                              |}""".stripMargin

  val credentialClaimsJson = io.circe.parser
    .parse(credentialClaimsStr)
    .getOrElse(throw new RuntimeException("Invalid json"))

  val unsignedCredential = UnsignedCredentialBuilder[JsonBasedUnsignedCredential]
    .buildFrom(
      issuerDID = did,
      issuanceKeyId = firstPublicKey.id,
      claims = credentialClaimsJson
    )

  val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, masterKeyPair.privateKey)(EC)
}
