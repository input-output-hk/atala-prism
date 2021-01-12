package io.iohk.atala.prism.node.poc

import com.github.ghik.silencer.silent
import io.iohk.atala.prism.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._

// This SDK would allow to build generic credentials and manipulate them
// For this toy example, the credential model is a String that represents a JSON
// and we didn't add nice builders, we just take fixed values for illustration
// to build a degree credential
object GenericCredentialsSDK {

  private var issuerDIDUsed: DID = _
  private var keyIdUsed: String = ""

  def buildGenericCredential(
      credentialType: String,
      issuerDID: DID,
      issuanceKeyId: String,
      claims: String
  ): CredentialContent = {
    issuerDIDUsed = issuerDID
    keyIdUsed = issuanceKeyId
    CredentialContent(
      CredentialContent.JsonFields.CredentialType.field -> credentialType,
      CredentialContent.JsonFields.IssuerDid.field -> s"did:prism:${issuerDID.suffix}",
      CredentialContent.JsonFields.IssuanceKeyId.field -> issuanceKeyId,
      CredentialContent.JsonFields.CredentialSubject.field -> claims
    )
  }

  @silent("never used")
  def getIssuerDID(credential: String): String = issuerDIDUsed.value
  @silent("never used")
  def getIssuerDIDSufix(credential: String): DIDSuffix = issuerDIDUsed.suffix
  @silent("never used")
  def getKeyId(credential: String): String = keyIdUsed
}
