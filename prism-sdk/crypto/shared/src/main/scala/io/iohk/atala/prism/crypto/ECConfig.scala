package io.iohk.atala.prism.crypto

object ECConfig {
  val CURVE_NAME = "secp256k1"
  val CURVE_FIELD_BYTE_SIZE = 32 // EC curve point coordinates are 32 bytes long
  val SIGNATURE_ALGORITHM = "SHA256withECDSA"

  // Field characteristic p (prime) is equal to 2^256 - 2^32 - 2^9 - 2^8 - 2^7 - 2^6 - 2^4 - 1
  val p = BigInt("115792089237316195423570985008687907853269984665640564039457584007908834671663")
  val b = BigInt(7)
}
