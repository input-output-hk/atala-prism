package io.iohk.atala.prism.kotlin.crypto

object ECConfig {
    val CURVE_NAME = "secp256k1"
    val PRIVATE_KEY_BYTE_SIZE = 32 // EC curve point coordinates are 32 bytes long
    val PUBLIC_KEY_BYTE_SIZE = 65
    val SIGNATURE_ALGORITHM = "SHA256withECDSA"
}
