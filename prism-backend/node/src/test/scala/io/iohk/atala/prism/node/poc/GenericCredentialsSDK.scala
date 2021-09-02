package io.iohk.atala.prism.node.poc

import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import kotlinx.serialization.json.JsonElementKt.JsonPrimitive
import kotlinx.serialization.json.JsonObject

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
// This SDK would allow to build generic credentials and manipulate them
// For this toy example, the credential model is a String that represents a JSON
// and we didn't add nice builders, we just take fixed values for illustration
// to build a degree credential
object GenericCredentialsSDK {

  private var issuerPrismDidUsed: PrismDid = _
  private var keyIdUsed: String = ""

  def buildGenericCredential(
      credentialType: String,
      issuerPrismDid: PrismDid,
      issuanceKeyId: String,
      claims: String
  ): CredentialContent = {
    issuerPrismDidUsed = issuerPrismDid
    keyIdUsed = issuanceKeyId
    val fields = Map(
      "type" -> JsonPrimitive(credentialType),
      "id" -> JsonPrimitive(s"did:prism:${issuerPrismDid.getSuffix}"),
      "keyId" -> JsonPrimitive(issuanceKeyId),
      "credentialSubject" -> JsonPrimitive(claims)
    )
    new CredentialContent(new JsonObject(fields.asJava))
  }

  @nowarn("cat=unused-params")
  def getIssuerPrismDid(credential: String): String = issuerPrismDidUsed.getValue
  @nowarn("cat=unused-params")
  def getIssuerPrismDidSufix(credential: String): String = issuerPrismDidUsed.getSuffix
  @nowarn("cat=unused-params")
  def getKeyId(credential: String): String = keyIdUsed
}
