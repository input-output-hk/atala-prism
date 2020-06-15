package io.iohk.node.poc.toyflow

import io.iohk.node.models.DIDSuffix

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
      issuianceKeyId: String,
      claims: String
  ): String = {
    issuerDIDUsed = issuerDID
    keyIdUsed = issuianceKeyId
    s"""{
       |  "credentialType" : "$credentialType",
       |  "issuerDID" : "did:prism:${issuerDID.suffix}",
       |  "signingKey" : {
       |     "type" : "DIDKey",
       |     "key" : "$issuianceKeyId"
       |  },
       |  "claims" : $claims
       |}""".stripMargin
  }

  def getIssuerDID(credential: String): String = s"did:prism:$issuerDIDUsed"
  def getIssuerDIDSufix(credential: String): DIDSuffix = issuerDIDUsed
  def getKeyId(credential: String) = keyIdUsed
}
