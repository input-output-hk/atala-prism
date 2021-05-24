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

actual object EC {
    private val ecjs = ec("secp256k1")

    actual fun generateKeyPair(): ECKeyPair {
        val keyPair = ecjs.genKeyPair()
        val basePoint = keyPair.getPublic()
        val bigNumber = keyPair.getPrivate()
        return ECKeyPair(ECPublicKey(basePoint), ECPrivateKey(bigNumber))
    }

    actual fun toPrivateKey(encoded: List<Byte>): ECPrivateKey {
        return toPrivateKey(BigInteger.fromByteArray(encoded.toByteArray(), Sign.POSITIVE))
    }

    actual fun toPrivateKey(d: BigInteger): ECPrivateKey {
        return ECPrivateKey(BN(d.toString()))
    }

    actual fun toPublicKey(encoded: List<Byte>): ECPublicKey {
        val encodedArray = encoded.toByteArray()
        val xBytes = encodedArray.copyOfRange(1, 1 + ECConfig.PRIVATE_KEY_BYTE_SIZE)
        val yBytes = encodedArray.copyOfRange(1 + ECConfig.PRIVATE_KEY_BYTE_SIZE, encoded.size)
        return toPublicKey(xBytes.toList(), yBytes.toList())
    }

    actual fun toPublicKey(
        x: List<Byte>,
        y: List<Byte>
    ): ECPublicKey {
        val xInteger = BigInteger.fromByteArray(x.toByteArray(), Sign.POSITIVE)
        val yInteger = BigInteger.fromByteArray(y.toByteArray(), Sign.POSITIVE)
        return toPublicKey(xInteger, yInteger)
    }

    actual fun toPublicKey(
        x: BigInteger,
        y: BigInteger
    ): ECPublicKey {
        val xCoord = x.toByteArray().toList().map { it.toUByte() }
        val yCoord = y.toByteArray().toList().map { it.toUByte() }
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

    actual fun toSignature(encoded: List<Byte>): ECSignature {
        return ECSignature(encoded.map { it.toUByte() })
    }

    actual fun sign(
        text: String,
        privateKey: ECPrivateKey
    ): ECSignature {
        return sign(text.encodeToByteArray().toList(), privateKey)
    }

    actual fun sign(
        data: List<Byte>,
        privateKey: ECPrivateKey
    ): ECSignature {
        val digest = bytesToHex(SHA256.compute(data).map { it.toUByte() })
        val signature = ecjs.sign(digest, privateKey.getHexEncoded(), enc = "hex")
        return ECSignature(signature.toDER(enc = "hex").unsafeCast<String>())
    }

    actual fun verify(
        text: String,
        publicKey: ECPublicKey,
        signature: ECSignature
    ): Boolean {
        return verify(text.encodeToByteArray().toList(), publicKey, signature)
    }

    actual fun verify(
        data: List<Byte>,
        publicKey: ECPublicKey,
        signature: ECSignature
    ): Boolean {
        val hexData = bytesToHex(SHA256.compute(data).map { it.toUByte() })
        return ecjs.verify(hexData, signature.sig, publicKey.getHexEncoded(), enc = "hex")
    }

    actual fun isSecp256k1(point: ECPoint): Boolean {
        return Secp256k1.isSecp256k1(point)
    }
}
