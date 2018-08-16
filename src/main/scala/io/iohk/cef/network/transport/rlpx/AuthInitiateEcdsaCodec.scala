package io.iohk.cef.network.transport.rlpx

import akka.util.ByteString
import io.iohk.cef.cryptolegacy.ECDSASignature
import io.iohk.cef.cryptolegacy.ECDSASignature.{RLength, SLength}
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray

trait AuthInitiateEcdsaCodec {

  def encodeECDSA(sig: ECDSASignature): ByteString = {
    import sig._

    val recoveryId: Byte = (v - 27).toByte

    ByteString(
      asUnsignedByteArray(r.bigInteger).reverse.padTo(RLength, 0.toByte).reverse ++
        asUnsignedByteArray(s.bigInteger).reverse.padTo(SLength, 0.toByte).reverse ++
        Array(recoveryId))
  }

  def decodeECDSA(input: Array[Byte]): ECDSASignature = {
    val SIndex = 32
    val VIndex = 64

    val r = input.take(RLength)
    val s = input.slice(SIndex, SIndex + SLength)
    val v = input(VIndex) + 27
    ECDSASignature(BigInt(1, r), BigInt(1, s), v.toByte)
  }
}
