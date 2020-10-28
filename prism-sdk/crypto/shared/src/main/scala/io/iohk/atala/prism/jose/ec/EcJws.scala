package io.iohk.atala.prism.jose.ec

import java.{util => ju}

import io.circe.Encoder
import io.circe.syntax._

import io.iohk.atala.prism.crypto.{ECTrait, ECPrivateKey, ECPublicKey, ECSignature}
import io.iohk.atala.prism.jose.{Jws, JwsContent, JwsHeader, JwsSignature}

case class EcJwsContent[P](protectedHeader: JwsHeader[EcJwk], payload: P)
    extends JwsContent[JwsHeader[EcJwk], Nothing, P] {

  override def unprotectedHeader = None // not used

  def sign(privateKey: ECPrivateKey)(implicit ec: ECTrait, ep: Encoder[P], eh: Encoder[JwsHeader[EcJwk]]): EcJws[P] = {
    new EcJws(
      content = this,
      signatures = Seq(JwsSignature(encodedProtectedHeader, ec.sign(encoded, privateKey)))
    )
  }

  def encoded(implicit ep: Encoder[P], eh: Encoder[JwsHeader[EcJwk]]): String =
    s"${encodedProtectedHeader}.$encodedPayload"

  def encodedPayload(implicit ep: Encoder[P]): String = {
    val asString = payload.asJson.dropNullValues.noSpaces

    if (protectedHeader.b64.contains(true))
      ju.Base64.getUrlEncoder.withoutPadding.encodeToString(asString.getBytes)
    else
      asString
  }

  def encodedProtectedHeader(implicit eh: Encoder[JwsHeader[EcJwk]]): String =
    ju.Base64.getUrlEncoder.withoutPadding.encodeToString(protectedHeader.asJson.dropNullValues.noSpaces.getBytes)
}

case class EcJws[P](
    content: EcJwsContent[P],
    signatures: Seq[JwsSignature[ECSignature]]
) extends Jws[JwsHeader[EcJwk], Nothing, P, ECSignature] {

  def isValidSignature(
      publicKey: ECPublicKey
  )(implicit ec: ECTrait, ep: Encoder[P]): Boolean =
    signatures.forall(signature =>
      ec.verify(s"${signature.`protected`}.${content.encodedPayload}", publicKey, signature.signature)
    )

}
