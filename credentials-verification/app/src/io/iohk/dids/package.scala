package io.iohk

import java.security.{PublicKey => JPublicKey}
import java.util.Base64

import enumeratum._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.dids.security.DIDPublicKeyType
import org.bouncycastle.jcajce.provider.asymmetric.ec.{BCECPrivateKey, BCECPublicKey}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECCurve
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

package object dids {

  object JWKUtils {
    // https://tools.ietf.org/html/rfc7518#section-7.6.2
    // https://tools.ietf.org/id/draft-jones-webauthn-secp256k1-00.html
    val jwkSupportedCurves = Map("P-256" -> "P-256", "P-384" -> "P-384", "P-521" -> "P-521", "P-256K" -> "secp256k1")
    private val curveNamesRMap = jwkSupportedCurves.map {
      case (jwkName, bcName) => (ECNamedCurveTable.getParameterSpec(bcName).getCurve, jwkName)
    }.toMap

    def getCurveName(curve: ECCurve): String = curveNamesRMap(curve)

    def encodeBigInt(bi: BigInt): String = {
      val bytes = bi.bigInteger.toByteArray

      val fixedBytes = if (bytes(0) == 0) {
        // Java prepends some encoded BigIntegers with 0 to differentiate them
        // from signed ones; EC values are always positive, so they don't need
        // it and are always encoded as unsigned
        bytes.drop(1)
      } else {
        bytes
      }

      Base64.getUrlEncoder.encodeToString(fixedBytes)
    }

    sealed trait KeyUse extends EnumEntry
    object KeyUse extends Enum[KeyUse] {
      val values = findValues

      // https://tools.ietf.org/html/rfc7517#section-8.2
      case object sig extends KeyUse
      case object enc extends KeyUse
    }

    sealed trait KeyOp extends EnumEntry
    object KeyOp extends Enum[KeyOp] {
      val values = findValues

      case object sign extends KeyOp
      case object verify extends KeyOp
      case object encrypt extends KeyOp
      case object decrypt extends KeyOp
      case object wrapKey extends KeyOp
      case object unwrapKey extends KeyOp
      case object deriveKey extends KeyOp
      case object deriveBits extends KeyOp
    }

  }

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

  object JWKPublicKey {
    def fromBCECPublicKey(key: BCECPublicKey, use: JWKUtils.KeyUse, keyId: String, algorithm: String): JWKPublicKey = {
      val (defaultEncryptionAlgorithm, defaultSignAlgorithm) = use match {
        case JWKUtils.KeyUse.sig => ("none", algorithm)
        case JWKUtils.KeyUse.enc => (algorithm, "none")
      }
      new JWKPublicKey(
        kty = "EC",
        kid = keyId,
        crv = JWKUtils.getCurveName(key.getParameters.getCurve),
        x = JWKUtils.encodeBigInt(key.getW.getAffineX),
        y = JWKUtils.encodeBigInt(key.getW.getAffineY),
        use = use.toString,
        defaultEncryptionAlgorithm = defaultEncryptionAlgorithm,
        defaultSignAlgorithm = defaultSignAlgorithm
      )
    }
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

  object JWKPrivateKey {

    def fromBCPrivateKey(key: BCECPrivateKey, keyId: String, keyOps: List[JWKUtils.KeyOp]): JWKPrivateKey = {
      JWKPrivateKey(
        kty = key.getAlgorithm,
        kid = keyId,
        crv = JWKUtils.getCurveName(key.getParameters.getCurve()),
        d = JWKUtils.encodeBigInt(key.getD),
        key_ops = keyOps.map(_.toString),
        defaultEncryptionAlgorithm = "",
        defaultSignAlgorithm = ""
      )
    }

  }

  case class PublicKey(id: String, `type`: String, publicKeyJwk: JWKPublicKey)

  object PublicKey {
    def fromJavaPublicKey(
        id: String,
        key: JPublicKey,
        didType: DIDPublicKeyType,
        algorithm: String,
        use: JWKUtils.KeyUse
    ): PublicKey = {
      require(didType.algorithms.contains(algorithm))

      key match {
        case key: BCECPublicKey =>
          PublicKey(id, didType.name, JWKPublicKey.fromBCECPublicKey(key, use, id, algorithm))
        case _ =>
          throw new IllegalArgumentException(s"Unsupported key type: ${key.getClass.getCanonicalName}")
      }
    }

    def toJavaPublicKey(key: PublicKey): Try[JPublicKey] = key match {
      case PublicKey(_, _, jwkKey @ JWKPublicKey("EC", _, crv, _, _, _, _, _)) =>
        if (JWKUtils.jwkSupportedCurves.get(crv).contains(ECKeys.CURVE_NAME)) {
          Success(ECKeys.toPublicKey(jwkKey.xBytes, jwkKey.yBytes))
        } else {
          Failure(new IllegalArgumentException("Unsupported EC curve"))
        }
      case _ =>
        Failure(new IllegalArgumentException("Unsupported key type"))
    }

    def signingKey(keyId: String, key: JPublicKey, keyType: DIDPublicKeyType, algorithm: String): PublicKey = {
      fromJavaPublicKey(keyId, key, keyType, algorithm, JWKUtils.KeyUse.sig)
    }

    def signingKey(keyId: String, key: JPublicKey, keyType: DIDPublicKeyType): PublicKey = {
      signingKey(keyId, key, keyType, keyType.algorithms.head)
    }

    def encryptionKey(keyId: String, key: JPublicKey, keyType: DIDPublicKeyType): PublicKey = {
      encryptionKey(keyId, key, keyType, keyType.algorithms.head)
    }

    def encryptionKey(keyId: String, key: JPublicKey, keyType: DIDPublicKeyType, algorithm: String): PublicKey = {
      fromJavaPublicKey(keyId, key, keyType, algorithm, JWKUtils.KeyUse.enc)
    }
  }

  case class Document(id: String, publicKey: List[PublicKey])

  private implicit val jwtPublicKeyFormat: Format[JWKPublicKey] =
    Json.format[JWKPublicKey]

  implicit val jwtPrivateKeyFormat: Format[JWKPrivateKey] =
    Json.format[JWKPrivateKey]

  private implicit val publicKeyFormat: Format[PublicKey] = Json.format[PublicKey]

  implicit val documentFormat: Format[Document] = OFormat.apply(
    Json.reads[Document],
    Json.writes[Document].transform { js: JsObject =>
      js.+("@context" -> JsString("https://w3id.org/did/v1"))
    }
  )
}
