package io.iohk.atala.prism.node.poc.toyflow

import com.github.ghik.silencer.silent
import io.iohk.atala.prism.node.models.DIDSuffix

// This SDK would allow to build generic credentials and manipulate them
// For this toy example, the credential model is a String that represents a JSON
// and we didn't add nice builders, we just take fixed values for illustration
// to build a degree credential
object GenericCredentialsSDK {

  private var issuerDIDUsed: DIDSuffix = _
  private var keyIdUsed: String = ""

  def buildGenericCredential(
      credentialType: String,
      issuerDID: DIDSuffix,
      issuanceKeyId: String,
      claims: String
  ): String = {
    issuerDIDUsed = issuerDID
    keyIdUsed = issuanceKeyId
    s"""{
       |  "credentialType" : "$credentialType",
       |  "issuerDID" : "did:prism:${issuerDID.suffix}",
       |  "signingKey" : {
       |     "type" : "DIDKey",
       |     "key" : "$issuanceKeyId"
       |  },
       |  "claims" : $claims
       |}""".stripMargin
  }

  @silent("never used")
  def getIssuerDID(credential: String): String = s"did:prism:$issuerDIDUsed"
  @silent("never used")
  def getIssuerDIDSufix(credential: String): DIDSuffix = issuerDIDUsed
  @silent("never used")
  def getKeyId(credential: String) = keyIdUsed
}
