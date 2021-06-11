package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.prism.kotlin.crypto.ECConfig.CURVE_NAME
import io.iohk.atala.prism.kotlin.crypto.ECConfig.SIGNATURE_ALGORITHM
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPoint
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.util.toJavaBigInteger
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import java.security.*
import java.security.spec.*

actual object EC {
    private val provider = GenericJavaCryptography.provider
    private val ecNamedCurveSpec = GenericJavaCryptography.ecNamedCurveSpec
    private val keyFactory = KeyFactory.getInstance("EC", provider)

    init {
        Security.addProvider(provider)
    }

    @JvmStatic
    actual fun generateKeyPair(): ECKeyPair {
        val keyGen = KeyPairGenerator.getInstance("ECDSA", provider)
        val ecSpec = ECGenParameterSpec(CURVE_NAME)
        keyGen.initialize(ecSpec, SecureRandom())
        val keyPair = keyGen.generateKeyPair()
        return ECKeyPair(ECPublicKey(keyPair.public), ECPrivateKey(keyPair.private))
    }

    @JvmStatic
    actual fun toPrivateKey(encoded: ByteArray): ECPrivateKey {
        return toPrivateKey(BigInteger.fromByteArray(encoded, Sign.POSITIVE))
    }

    @JvmStatic
    actual fun toPrivateKey(d: BigInteger): ECPrivateKey {
        val spec = ECPrivateKeySpec(d.toJavaBigInteger(), ecNamedCurveSpec)
        return ECPrivateKey(keyFactory.generatePrivate(spec))
    }

    @JvmStatic
    actual fun toPublicKey(encoded: ByteArray): ECPublicKey {
        val expectedLength = 1 + 2 * ECConfig.PRIVATE_KEY_BYTE_SIZE
        assert(encoded.size == expectedLength) {
            "Encoded byte array's expected length is $expectedLength, but got ${encoded.size}"
        }
        assert(encoded[0].toInt() == 4) {
            "First byte was expected to be 4, but got ${encoded[0]}"
        }

        val xBytes = encoded.copyOfRange(1, 1 + ECConfig.PRIVATE_KEY_BYTE_SIZE)
        val yBytes = encoded.copyOfRange(1 + ECConfig.PRIVATE_KEY_BYTE_SIZE, encoded.size)
        return toPublicKey(xBytes, yBytes)
    }

    @JvmStatic
    actual fun toPublicKey(x: ByteArray, y: ByteArray): ECPublicKey {
        return toPublicKey(x.toKotlinBigInteger(), y.toKotlinBigInteger())
    }

    @JvmStatic
    actual fun toPublicKey(x: BigInteger, y: BigInteger): ECPublicKey {
        val ecPoint = ECPoint(x.toJavaBigInteger(), y.toJavaBigInteger())
        val spec = ECPublicKeySpec(ecPoint, ecNamedCurveSpec)
        return ECPublicKey(keyFactory.generatePublic(spec))
    }

    @JvmStatic
    actual fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey {
        val pubSpec = GenericJavaCryptography.keySpec(privateKey.getD().toJavaBigInteger())
        return ECPublicKey(keyFactory.generatePublic(pubSpec))
    }

    @JvmStatic
    actual fun toSignature(encoded: ByteArray): ECSignature {
        return ECSignature(encoded)
    }

    @JvmStatic
    actual fun sign(text: String, privateKey: ECPrivateKey): ECSignature {
        return sign(text.toByteArray(), privateKey)
    }

    @JvmStatic
    actual fun sign(data: ByteArray, privateKey: ECPrivateKey): ECSignature {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM, provider)
        signer.initSign(privateKey.key)
        signer.update(data)
        return ECSignature(signer.sign())
    }

    @JvmStatic
    actual fun verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        return verify(text.toByteArray(), publicKey, signature)
    }

    @JvmStatic
    actual fun verify(data: ByteArray, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, provider)
        verifier.initVerify(publicKey.key)
        verifier.update(data)
        return verifier.verify(signature.data)
    }

    @JvmStatic
    actual fun isSecp256k1(point: ECPoint): Boolean {
        return Secp256k1.isSecp256k1(point)
    }
}
