package io.iohk.cvp.utils

import android.util.Base64
import io.grpc.Metadata
import io.iohk.atala.prism.crypto.MnemonicChecksumException
import io.iohk.atala.prism.crypto.MnemonicLengthException
import io.iohk.atala.prism.crypto.MnemonicWordException
import io.iohk.atala.prism.crypto.japi.*
import java.util.*

class CryptoUtils {

    companion object {

        fun getMetadata(ecKeyPair: ECKeyPair, data: ByteArray) : Metadata {
            val nonce = BytesConverterUtil.getBytesFromUUID(UUID.randomUUID())

            val instance = EC.getInstance(CryptoProvider.Android)
            val firm = instance.sign(nonce!!.plus(data), ecKeyPair.private)

            val metadata = Metadata()

            metadata.put(GrpcUtils.SIGNATURE_KEY, Base64.encodeToString(firm.data, Base64.URL_SAFE or Base64.NO_WRAP))
            metadata.put(GrpcUtils.PUBLIC_KEY, Base64.encodeToString(ecKeyPair.public.encoded, Base64.URL_SAFE or Base64.NO_WRAP))
            metadata.put(GrpcUtils.REQUEST_NONCE_KEY, Base64.encodeToString(nonce, Base64.URL_SAFE or Base64.NO_WRAP))

            return metadata
        }

        fun getNextPathFromIndex(index: Int) : String {
            return "m/${index + 1}'/0'/0'"
        }

        fun getPathFromIndex(index: Int) : String {
            return "m/${index}'/0'/0'"
        }
        fun getKeyPairFromPath(keyDerivationPath: String, phrases: List<String>) : ECKeyPair {
            val derivationPath = DerivationPath.parse(keyDerivationPath)
            val mnemonicCode = MnemonicCode(phrases)
            val keyDerivation = KeyDerivation.getInstance(CryptoProvider.Android)
            val seed = keyDerivation.binarySeed(mnemonicCode, "")
            return keyDerivation.deriveKey(seed, derivationPath).keyPair
        }

        fun isValidMnemonicList(phrases: List<String>) : Boolean {
            val ec = KeyDerivation.getInstance(CryptoProvider.Android)
            val mnemonicCode = MnemonicCode(phrases)
            ec.binarySeed(mnemonicCode, "")
            return phrases.all { word: String ->  ec.isValidMnemonicWord(word)}
        }

        fun generateMnemonicList() : MutableList<String> {
            val key = KeyDerivation.getInstance(CryptoProvider.Android)
            return key.randomMnemonicCode().words
        }
    }
}