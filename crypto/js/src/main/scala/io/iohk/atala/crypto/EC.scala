package io.iohk.atala.crypto

import io.iohk.atala.crypto.ECConfig.CURVE_NAME
import io.iohk.atala.crypto.ECUtils.toHex
import typings.bnJs.mod.^
import typings.elliptic.AnonX
import typings.elliptic.mod.curve.base.BasePoint
import typings.elliptic.mod.ec
import typings.elliptic.mod.ec.KeyPair
import typings.hashJs.{mod => hash}

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.typedarray.{Uint8Array, _}

/**
  * JavaScript implementation of {@link ECTrait}.
  */
object EC extends ECTrait {
  private val HEX_ENC = "hex"
  private val nativeEc = new ec(CURVE_NAME)

  override def generateKeyPair(): ECKeyPair = {
    val nativeKeyPair = nativeEc.genKeyPair()
    ECKeyPair(new JsECPrivateKey(nativeKeyPair.getPrivate()), new JsECPublicKey(nativeKeyPair.getPublic()))
  }

  override def toPrivateKey(d: BigInt): ECPrivateKey = {
    new JsECPrivateKey(nativeEc.keyFromPrivate(toHex(d), HEX_ENC).getPrivate())
  }

  override def toPublicKey(x: BigInt, y: BigInt): ECPublicKey = {
    new JsECPublicKey(
      nativeEc
        .keyFromPublic(AnonX(x = toHex(x), y = toHex(y)))
        .getPublic()
    )
  }

  override def toPublicKeyFromPrivateKey(d: BigInt): ECPublicKey = {
    new JsECPublicKey(nativeEc.keyFromPrivate(toHex(d), HEX_ENC).getPublic())
  }

  override def sign(data: Array[Byte], privateKey: ECPrivateKey): ECSignature = {
    privateKey match {
      case key: JsECPrivateKey =>
        val signature = nativeEc.sign(sha256(data), asKeyPair(key.privateKey))
        val hexSignature = signature.toDER(HEX_ENC).toString
        ECSignature(ECUtils.toUnsignedByteArray(ECUtils.toBigInt(hexSignature)))
    }
  }

  override def verify(data: Array[Byte], publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    publicKey match {
      case key: JsECPublicKey =>
        nativeEc.verify(
          sha256(data),
          ECUtils.bytesToHex(signature.data),
          asKeyPair(key.publicKey)
        )
    }
  }

  private def sha256(bytes: Array[Byte]): js.Array[Double] = {
    val uint8Array = new Uint8Array(bytes.toTypedArray.asInstanceOf[Uint8Array])
    val sha256 = hash.sha256().update(uint8Array)
    sha256.digest()
  }

  private def asKeyPair(key: ^ | BasePoint): KeyPair = {
    // The type can be enforced as the JS implementation can figure it out at runtime
    key.asInstanceOf[KeyPair]
  }
}
