package io.iohk.atala.prism.jose.ec

import java.{util => ju}

import io.iohk.atala.prism.crypto.{ECTrait, ECPublicKey, ECConfig}
import io.iohk.atala.prism.util.BigIntOps
import io.iohk.atala.prism.jose.Jwk

/**
  * EC based JWK.
  *
  * @param crv Cryptographic curve used with the key
  * @param x x coordinate for the elliptic curve point
  * @param y y coordinate for the elliptic curve point
  * @param didId The "kid" (Key ID) member can be used to match a specific key,
  *              in our case this will be the DID.
  */
case class EcJwk(crv: String, x: String, y: String, didId: Option[String])
    extends Jwk(
      kty = "EC",
      use = Some("sig"),
      keyOps = None,
      alg = None,
      kid = didId,
      x5u = None,
      x5c = None,
      x5t = None,
      `x5t#S256` = None
    ) {

  def publicKey(implicit ec: ECTrait): ECPublicKey = {
    ec.toPublicKey(
      BigIntOps.toBigInt(ju.Base64.getUrlDecoder.decode(x.getBytes)),
      BigIntOps.toBigInt(ju.Base64.getUrlDecoder.decode(y.getBytes))
    )
  }
}

object EcJwk {
  def apply(publicKey: ECPublicKey, didId: Option[String]): EcJwk = {
    val point = publicKey.getCurvePoint

    EcJwk(
      crv = ECConfig.CURVE_NAME,
      x = ju.Base64.getUrlEncoder.withoutPadding.encodeToString(BigIntOps.toUnsignedByteArray(point.x)),
      y = ju.Base64.getUrlEncoder.withoutPadding.encodeToString(BigIntOps.toUnsignedByteArray(point.y)),
      didId = didId
    )
  }
}
