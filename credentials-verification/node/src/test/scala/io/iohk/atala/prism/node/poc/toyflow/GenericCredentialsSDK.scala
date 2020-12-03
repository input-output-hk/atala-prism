package io.iohk.atala.prism.node.poc.toyflow

import com.github.ghik.silencer.silent
import io.iohk.atala.prism.identity.{DID, DIDSuffix}

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
  ): String = {
    issuerDIDUsed = issuerDID
    keyIdUsed = issuanceKeyId
    s"""{
       |  "credentialType" : "$credentialType",
       |  "issuerDID" : "${issuerDID.value}",
       |  "signingKey" : {
       |     "type" : "DIDKey",
       |     "key" : "$issuanceKeyId"
       |  },
       |  "claims" : $claims
       |}""".stripMargin
  }

  @silent("never used")
  def getIssuerDID(credential: String): String = issuerDIDUsed.value
  @silent("never used")
  def getIssuerDIDSufix(credential: String): DIDSuffix = issuerDIDUsed.suffix
  @silent("never used")
  def getKeyId(credential: String): String = keyIdUsed
}
