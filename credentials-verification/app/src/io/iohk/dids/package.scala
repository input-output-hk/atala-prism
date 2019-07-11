package io.iohk

import java.util.Base64

import play.api.libs.json.{Json, Reads}

package object dids {

  case class JWKPublicKey(
      kty: String,
      kid: String,
      crv: String,
      x: String,
      y: String,
      use: String,
      defaultEncryptionAlgorithm: String,
      defaultSignAlgorithm: String
  ) {

    def xBytes: Array[Byte] = Base64.getUrlDecoder.decode(x)
    def yBytes: Array[Byte] = Base64.getUrlDecoder.decode(y)
  }

  case class JWKPrivateKey(
      kty: String,
      kid: String,
      crv: String,
      d: String,
      key_ops: List[String],
      defaultEncryptionAlgorithm: String,
      defaultSignAlgorithm: String
  ) {

    def dBytes: Array[Byte] = Base64.getUrlDecoder.decode(d)
  }

  case class PublicKey(id: String, `type`: String, publicKeyJwk: JWKPublicKey)

  case class Document(id: String, publicKey: List[PublicKey])

  private implicit val jwtPublicKeyReads: Reads[JWKPublicKey] =
    Json.reads[JWKPublicKey]

  implicit val jwtPrivateKeyReads: Reads[JWKPrivateKey] =
    Json.reads[JWKPrivateKey]

  private implicit val publicKeyReads: Reads[PublicKey] = Json.reads[PublicKey]

  implicit val documentReads: Reads[Document] = Json.reads[Document]
}
