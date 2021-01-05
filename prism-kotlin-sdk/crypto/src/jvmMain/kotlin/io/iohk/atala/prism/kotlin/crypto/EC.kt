package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.prism.kotlin.crypto.ECConfig.CURVE_NAME
import io.iohk.atala.prism.kotlin.crypto.ECConfig.SIGNATURE_ALGORITHM
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.util.toJavaBigInteger
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import io.iohk.atala.prism.kotlin.util.toByteArray
import io.iohk.atala.prism.kotlin.util.toUByteArray
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECNamedCurveSpec
import java.security.*
import java.security.spec.*

actual object EC {
    private val provider = BouncyCastleProvider()
    private val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
    private val ecNamedCurveSpec: ECParameterSpec = ECNamedCurveSpec(
        ecParameterSpec.name,
        ecParameterSpec.curve,
        ecParameterSpec.g,
        ecParameterSpec.n
    )

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
    actual fun toPrivateKey(encoded: List<Byte>): ECPrivateKey {
        return toPrivateKey(BigInteger.fromByteArray(encoded.toByteArray(), Sign.POSITIVE))
    }

    @JvmStatic
    actual fun toPrivateKey(d: BigInteger): ECPrivateKey {
        val spec = ECPrivateKeySpec(d.toJavaBigInteger(), ecNamedCurveSpec)
        return ECPrivateKey(keyFactory.generatePrivate(spec))
    }

    @JvmStatic
    actual fun toPublicKey(encoded: List<Byte>): ECPublicKey {
        val expectedLength = 1 + 2 * ECConfig.PRIVATE_KEY_BYTE_SIZE
        assert(encoded.size == expectedLength) {
            "Encoded byte array's expected length is $expectedLength, but got ${encoded.size}"
        }
        assert(encoded[0].toInt() == 4) {
            "First byte was expected to be 4, but got ${encoded[0]}"
        }

        val encodedArray = encoded.toByteArray()
        val xBytes = encodedArray.copyOfRange(1, 1 + ECConfig.PRIVATE_KEY_BYTE_SIZE)
        val yBytes = encodedArray.copyOfRange(1 + ECConfig.PRIVATE_KEY_BYTE_SIZE, encoded.size)
        return toPublicKey(xBytes.toList(), yBytes.toList())
    }

    @JvmStatic
    actual fun toPublicKey(x: List<Byte>, y: List<Byte>): ECPublicKey {
        return toPublicKey(x.toByteArray().toKotlinBigInteger(), y.toByteArray().toKotlinBigInteger())
    }

    @JvmStatic
    actual fun toPublicKey(x: BigInteger, y: BigInteger): ECPublicKey {
        val ecPoint = ECPoint(x.toJavaBigInteger(), y.toJavaBigInteger())
        val spec = ECPublicKeySpec(ecPoint, ecNamedCurveSpec)
        return ECPublicKey(keyFactory.generatePublic(spec))
    }

    @JvmStatic
    actual fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey {
        val q = ecParameterSpec.g.multiply(privateKey.getD().toJavaBigInteger())
        val pubSpec = org.spongycastle.jce.spec.ECPublicKeySpec(q, ecParameterSpec)
        return ECPublicKey(keyFactory.generatePublic(pubSpec))
    }

    @ExperimentalUnsignedTypes
    @JvmStatic
    actual fun toSignature(encoded: List<Byte>): ECSignature {
        return ECSignature(encoded.map { it.toUByte() })
    }

    @ExperimentalUnsignedTypes
    @JvmStatic
    actual fun sign(text: String, privateKey: ECPrivateKey): ECSignature {
        return sign(text.toByteArray().toList(), privateKey)
    }

    @ExperimentalUnsignedTypes
    @JvmStatic
    actual fun sign(data: List<Byte>, privateKey: ECPrivateKey): ECSignature {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM, provider)
        signer.initSign(privateKey.key)
        signer.update(data.toByteArray())
        return ECSignature(signer.sign().toUByteArray().toList())
    }

    @ExperimentalUnsignedTypes
    @JvmStatic
    actual fun verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        return verify(text.toByteArray().toList(), publicKey, signature)
    }

    @ExperimentalUnsignedTypes
    @JvmStatic
    actual fun verify(data: List<Byte>, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, provider)
        verifier.initVerify(publicKey.key)
        verifier.update(data.toByteArray())
        return verifier.verify(signature.data.toByteArray())
    }
}
