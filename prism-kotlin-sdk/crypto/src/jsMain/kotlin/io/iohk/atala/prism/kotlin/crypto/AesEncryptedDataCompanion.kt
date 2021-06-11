package io.iohk.atala.prism.kotlin.crypto

@JsExport
object AesEncryptedDataCompanion {
    fun fromCombined(bytes: ByteArray): AesEncryptedData =
        AesEncryptedData.fromCombined(bytes)
}
