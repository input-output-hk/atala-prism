package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.prism.kotlin.crypto.externals.BN
import io.iohk.atala.prism.kotlin.crypto.externals.Coordinates
import io.iohk.atala.prism.kotlin.crypto.externals.ec
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPoint
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex

@JsExport
actual object EC {
    private val ecjs = ec("secp256k1")

    actual fun generateKeyPair(): ECKeyPair {
        val keyPair = ecjs.genKeyPair()
        val basePoint = keyPair.getPublic()
        val bigNumber = keyPair.getPrivate()
        return ECKeyPair(ECPublicKey(basePoint), ECPrivateKey(bigNumber))
    }

    @JsName("toPrivateKeyFromBytes")
    actual fun toPrivateKey(encoded: ByteArray): ECPrivateKey {
        return toPrivateKey(BigInteger.fromByteArray(encoded, Sign.POSITIVE))
    }

    @JsName("toPrivateKeyFromBigInteger")
    actual fun toPrivateKey(d: BigInteger): ECPrivateKey {
        return ECPrivateKey(BN(d.toString()))
    }

    @JsName("toPublicKeyFromBytes")
    actual fun toPublicKey(encoded: ByteArray): ECPublicKey {
        val xBytes = encoded.copyOfRange(1, 1 + ECConfig.PRIVATE_KEY_BYTE_SIZE)
        val yBytes = encoded.copyOfRange(1 + ECConfig.PRIVATE_KEY_BYTE_SIZE, encoded.size)
        return toPublicKey(xBytes, yBytes)
    }

    @JsName("toPublicKeyFromByteCoordinates")
    actual fun toPublicKey(
        x: ByteArray,
        y: ByteArray
    ): ECPublicKey {
        val xInteger = BigInteger.fromByteArray(x, Sign.POSITIVE)
        val yInteger = BigInteger.fromByteArray(y, Sign.POSITIVE)
        return toPublicKey(xInteger, yInteger)
    }

    @JsName("toPublicKeyFromBigIntegerCoordinates")
    actual fun toPublicKey(
        x: BigInteger,
        y: BigInteger
    ): ECPublicKey {
        val xCoord = x.toByteArray()
        val yCoord = y.toByteArray()
        val keyPair = ecjs.keyFromPublic(
            object : Coordinates {
                override var x = bytesToHex(xCoord)
                override var y = bytesToHex(yCoord)
            }
        )
        return ECPublicKey(keyPair.getPublic())
    }

    actual fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey {
        val keyPair = ecjs.keyFromPrivate(privateKey.bigNumber.toString("hex"))
        return ECPublicKey(keyPair.getPublic())
    }

    actual fun toSignature(encoded: ByteArray): ECSignature {
        return ECSignature(encoded)
    }

    actual fun sign(
        text: String,
        privateKey: ECPrivateKey
    ): ECSignature {
        return sign(text.encodeToByteArray(), privateKey)
    }

    @JsName("signBytes")
    actual fun sign(
        data: ByteArray,
        privateKey: ECPrivateKey
    ): ECSignature {
        val digest = bytesToHex(SHA256.compute(data))
        val signature = ecjs.sign(digest, privateKey.getHexEncoded(), enc = "hex")
        return ECSignature(signature.toDER(enc = "hex").unsafeCast<String>())
    }

    actual fun verify(
        text: String,
        publicKey: ECPublicKey,
        signature: ECSignature
    ): Boolean {
        return verify(text.encodeToByteArray(), publicKey, signature)
    }

    @JsName("verifyBytes")
    actual fun verify(
        data: ByteArray,
        publicKey: ECPublicKey,
        signature: ECSignature
    ): Boolean {
        val hexData = bytesToHex(SHA256.compute(data))
        return ecjs.verify(hexData, signature.sig, publicKey.getHexEncoded(), enc = "hex")
    }

    actual fun isSecp256k1(point: ECPoint): Boolean {
        return Secp256k1.isSecp256k1(point)
    }
}
