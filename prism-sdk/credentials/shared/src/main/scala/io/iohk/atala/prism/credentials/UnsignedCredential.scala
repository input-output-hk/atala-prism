package io.iohk.atala.prism.credentials

import io.circe.Json
import io.circe.syntax._

trait UnsignedCredential {
  def json: Json
  def bytes: Array[Byte]
  def issuerDID: Option[String]
  def issuanceKeyId: Option[String]
}

class JsonBasedUnsignedCredential private (credentialBytes: Array[Byte]) extends UnsignedCredential {
  private val jsonEither = io.circe.parser.parse(credentialBytes.asString)

  require(
    jsonEither.isRight,
    s"""The credential bytes must represent a valid UTF-8 JSON
      |- Error: $jsonEither
      |- Array: ${credentialBytes.toVector.mkString("Array(", ",", ")")}
      |- The array represents the string: ${credentialBytes.asString}
      |""".stripMargin
  )

  lazy val json: Json = jsonEither.toOption.get

  lazy val bytes: Array[Byte] = credentialBytes

  lazy val issuerDID: Option[String] = {
    json.hcursor.get[String](JsonBasedUnsignedCredential.issuerDIDFieldName).toOption
  }

  lazy val issuanceKeyId: Option[String] = {
    json.hcursor.get[String](JsonBasedUnsignedCredential.keyIdFieldName).toOption
  }

  override def equals(obj: Any): Boolean = {
    canEqual(obj) && (obj match {
      case unsigned: UnsignedCredential => credentialBytes sameElements unsigned.bytes
      case _ => false
    })
  }

  private def canEqual(that: Any): Boolean = that.isInstanceOf[UnsignedCredential]
}

trait UnsignedCredentialBuilder[A] {
  def buildFrom(issuerDID: String, issuanceKeyId: String, claims: Json): UnsignedCredential
  def fromBytes(bytes: Array[Byte]): UnsignedCredential
}

object UnsignedCredentialBuilder {
  def apply[A: UnsignedCredentialBuilder](implicit
      builder: UnsignedCredentialBuilder[A]
  ): UnsignedCredentialBuilder[A] = builder

  def instance[A](
      buildFromF: (String, String, Json) => UnsignedCredential,
      bytesF: Array[Byte] => UnsignedCredential
  ): UnsignedCredentialBuilder[A] =
    new UnsignedCredentialBuilder[A] {
      override def buildFrom(issuerDID: String, issuanceKeyId: String, claims: Json): UnsignedCredential =
        buildFromF(issuerDID, issuanceKeyId, claims)

      override def fromBytes(bytes: Array[Byte]): UnsignedCredential = bytesF(bytes)
    }
}

object JsonBasedUnsignedCredential {
  private val keyIdFieldName = "keyId"
  private val issuerDIDFieldName = "issuer"
  private val claimsFieldName = "claims"

  implicit val jsonBasedUnsignedCredential: UnsignedCredentialBuilder[JsonBasedUnsignedCredential] =
    UnsignedCredentialBuilder.instance(
      (issuerDID: String, issuanceKeyId: String, claims: Json) =>
        new JsonBasedUnsignedCredential(
          Json
            .obj(
              issuerDIDFieldName -> issuerDID.asJson,
              keyIdFieldName -> issuanceKeyId.asJson,
              claimsFieldName -> claims
            )
            .noSpaces
            .getBytes(charsetUsed)
        ),
      new JsonBasedUnsignedCredential(_)
    )
}
