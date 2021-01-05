package io.iohk.atala.prism.app.utils

import android.util.Base64
import io.grpc.Metadata
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.derivation.*
import java.util.*

class CryptoUtils {

    companion object {

        @ExperimentalUnsignedTypes
        fun getMetadata(ecKeyPair: ECKeyPair, data: ByteArray): Metadata {
            val nonce = BytesConverterUtil.getBytesFromUUID(UUID.randomUUID())

            val firm = EC.sign(nonce!!.plus(data).toList(), ecKeyPair.privateKey)

            val metadata = Metadata()

            metadata.put(GrpcUtils.SIGNATURE_KEY, Base64.encodeToString(firm.getEncoded().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP))
            metadata.put(GrpcUtils.PUBLIC_KEY, Base64.encodeToString(ecKeyPair.publicKey.getEncoded().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP))
            metadata.put(GrpcUtils.REQUEST_NONCE_KEY, Base64.encodeToString(nonce, Base64.URL_SAFE or Base64.NO_WRAP))

            return metadata
        }

        fun getNextPathFromIndex(index: Int): String {
            return "m/${index + 1}'/0'/0'"
        }

        fun getPathFromIndex(index: Int): String {
            return "m/${index}'/0'/0'"
        }

        fun getKeyPairFromPath(keyDerivationPath: String, phrases: List<String>): ECKeyPair {
            val derivationPath = DerivationPath.fromPath(keyDerivationPath)
            val mnemonicCode = MnemonicCode(phrases)
            val seed = JvmKeyDerivation.binarySeed(mnemonicCode, "")
            return JvmKeyDerivation.deriveKey(seed, derivationPath).keyPair()
        }

        fun isValidMnemonicList(phrases: List<String>): Boolean {
            val mnemonicCode = MnemonicCode(phrases)
            JvmKeyDerivation.binarySeed(mnemonicCode, "")
            return phrases.all { word: String -> JvmKeyDerivation.isValidMnemonicWord(word) }
        }

        fun generateMnemonicList(): MutableList<String> {
            return JvmKeyDerivation.randomMnemonicCode().words.toMutableList()
        }
    }
}