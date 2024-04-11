package io.iohk.atala.prism.node.poc

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.models.DidSuffix
import kotlinx.serialization.json.JsonElementKt.JsonPrimitive
import kotlinx.serialization.json.JsonObject

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
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
    val fields = Map(
      "type" -> JsonPrimitive(credentialType),
      "id" -> JsonPrimitive(s"did:prism:${issuerDID.getSuffix}"),
      "keyId" -> JsonPrimitive(issuanceKeyId),
      "credentialSubject" -> JsonPrimitive(claims)
    )
    new CredentialContent(new JsonObject(fields.asJava))
  }

  @nowarn("cat=unused-params")
  def getIssuerDID(credential: String): String = issuerDIDUsed.getValue
  @nowarn("cat=unused-params")
  def getIssuerDIDSufix(credential: String): DidSuffix = DidSuffix(
    issuerDIDUsed.getSuffix
  )
  @nowarn("cat=unused-params")
  def getKeyId(credential: String): String = keyIdUsed
}
