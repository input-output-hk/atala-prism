package io.iohk.atala.prism.app.utils

import android.util.Base64
import io.grpc.Metadata
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.derivation.DerivationPath
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.kotlin.crypto.derivation.MnemonicCode
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import java.util.UUID

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
            return "m/$index'/0'/0'"
        }

        fun getKeyPairFromPath(keyDerivationPath: String, phrases: List<String>): ECKeyPair {
            val derivationPath = DerivationPath.fromPath(keyDerivationPath)
            val mnemonicCode = MnemonicCode(phrases)
            val seed = KeyDerivation.binarySeed(mnemonicCode, "")
            return KeyDerivation.deriveKey(seed, derivationPath).keyPair()
        }

        fun isValidMnemonicList(phrases: List<String>): Boolean {
            val mnemonicCode = MnemonicCode(phrases)
            KeyDerivation.binarySeed(mnemonicCode, "")
            return phrases.all { word: String -> KeyDerivation.isValidMnemonicWord(word) }
        }

        fun generateMnemonicList(): MutableList<String> {
            return KeyDerivation.randomMnemonicCode().words.toMutableList()
        }
    }
}
