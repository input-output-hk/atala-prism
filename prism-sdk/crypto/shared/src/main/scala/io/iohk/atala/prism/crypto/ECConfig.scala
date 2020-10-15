package io.iohk.atala.prism.crypto

object ECConfig {
  val CURVE_NAME = "secp256k1"
  val CURVE_FIELD_BYTE_SIZE = 32 // EC curve point coordinates are 32 bytes long
  val SIGNATURE_ALGORITHM = "SHA256withECDSA"
}
