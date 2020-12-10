import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
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

  lazy val credentialContent: CredentialContent =
    CredentialContent(
      CredentialContent.JsonFields.CredentialType.field -> CredentialContent
        .Values("VerifiableCredential", "RedlandIdCredential"),
      CredentialContent.JsonFields.IssuerDid.field -> DID.buildPrismDID("123456678abcdefg").value,
      CredentialContent.JsonFields.IssuanceKeyId.field -> "Issuance-0",
      CredentialContent.JsonFields.CredentialSubject.field -> CredentialContent.Fields(
        "name" -> "Jorge Lopez Portillo",
        "degree" -> "Bachelor's in Self-Sovereign Identity Development"
      )
    )

  lazy val credential: Credential = Credential.fromCredentialContent(credentialContent)

  val signedCredential = credential.sign(masterKeyPair.privateKey)(EC)
}
