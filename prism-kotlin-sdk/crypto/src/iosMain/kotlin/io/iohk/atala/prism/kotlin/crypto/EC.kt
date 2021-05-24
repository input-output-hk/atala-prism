package io.iohk.atala.prism.kotlin.crypto

import cocoapods.Secp256k1Kit.*
import com.ionspin.kotlin.bignum.integer.BigInteger
import fr.acinq.bitcoin.*
import fr.acinq.secp256k1.Secp256k1
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPoint
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.util.toCArrayPointer
import io.iohk.atala.prism.kotlin.crypto.util.toUByteArray
import kotlinx.cinterop.*
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

actual object EC {
    // secp256k1 private key is just a sequence of random bytes, hence
    // we ask /dev/urandom to produce the necessary amount of random bytes
    private fun generatePrivateKey(memScope: MemScope): CArrayPointer<UByteVar> {
        val privateKey = memScope.allocArray<UByteVar>(ECConfig.PRIVATE_KEY_BYTE_SIZE)
        val privateKeyPtr = privateKey.getPointer(memScope)
        val urandom = fopen("/dev/urandom", "rb") ?: error("No /dev/urandom on this device")
        try {
            fread(privateKeyPtr, 1.convert(), ECConfig.PRIVATE_KEY_BYTE_SIZE.convert(), urandom)
            for (n in 0 until ECConfig.PRIVATE_KEY_BYTE_SIZE) privateKey[n] = privateKeyPtr[n]
        } finally {
            fclose(urandom)
        }
        return privateKey
    }

    private fun createContext(memScope: MemScope, options: Int): CPointer<secp256k1_context>? {
        val context = secp256k1_context_create(options.convert())

        // Clean-up context by destroying it on scope closure
        memScope.defer {
            secp256k1_context_destroy(context)
        }

        return context
    }

    actual fun generateKeyPair(): ECKeyPair {
        return memScoped {
            val context = createContext(this, SECP256K1_CONTEXT_SIGN or SECP256K1_CONTEXT_VERIFY)
            val privateKey = generatePrivateKey(this)
            val publicKey = alloc<secp256k1_pubkey>()
            if (secp256k1_ec_pubkey_create(context, publicKey.ptr, privateKey) != 1) {
                error("Invalid private key")
            }

            val publicKeyBytes = publicKey.data.toUByteArray(ECConfig.PUBLIC_KEY_BYTE_SIZE)
            val privateKeyBytes = privateKey.toUByteArray(ECConfig.PRIVATE_KEY_BYTE_SIZE)
            ECKeyPair(ECPublicKey(publicKeyBytes), ECPrivateKey(privateKeyBytes))
        }
    }

    actual fun toPrivateKey(encoded: List<Byte>): ECPrivateKey {
        assert(encoded.size == ECConfig.PRIVATE_KEY_BYTE_SIZE) {
            "Encoded byte array's expected length is ${ECConfig.PRIVATE_KEY_BYTE_SIZE}, but got ${encoded.size}"
        }

        return ECPrivateKey(encoded.toUByteArray())
    }

    actual fun toPrivateKey(d: BigInteger): ECPrivateKey {
        return toPrivateKey(d.toByteArray().toList())
    }

    actual fun toPublicKey(encoded: List<Byte>): ECPublicKey {
        assert(encoded.size == ECConfig.PUBLIC_KEY_BYTE_SIZE) {
            "Encoded byte array's expected length is ${ECConfig.PUBLIC_KEY_BYTE_SIZE}, but got ${encoded.size}"
        }

        return memScoped {
            val context = createContext(this, SECP256K1_CONTEXT_SIGN or SECP256K1_CONTEXT_VERIFY)
            val pubkey = alloc<secp256k1_pubkey>()
            val input = encoded.toUByteArray().toCArrayPointer(this)
            val result = secp256k1_ec_pubkey_parse(context, pubkey.ptr, input, encoded.size.convert())
            if (result != 1) {
                error("Could not parse public key")
            }

            val publicKeyBytes = pubkey.data.toUByteArray(ECConfig.PUBLIC_KEY_BYTE_SIZE)
            ECPublicKey(publicKeyBytes)
        }
    }

    actual fun toPublicKey(x: List<Byte>, y: List<Byte>): ECPublicKey {
        return toPublicKey(listOf<Byte>(0x04) + x + y)
    }

    actual fun toPublicKey(x: BigInteger, y: BigInteger): ECPublicKey {
        return toPublicKey(x.toByteArray().toList(), y.toByteArray().toList())
    }

    actual fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey {
        return memScoped {
            val context = createContext(this, SECP256K1_CONTEXT_SIGN or SECP256K1_CONTEXT_VERIFY)
            val privkey = privateKey.getEncoded().toUByteArray().toCArrayPointer(this)
            val publicKey = alloc<secp256k1_pubkey>()
            if (secp256k1_ec_pubkey_create(context, publicKey.ptr, privkey) != 1) {
                error("Invalid private key")
            }

            val publicKeyBytes = publicKey.data.toUByteArray(ECConfig.PUBLIC_KEY_BYTE_SIZE)
            ECPublicKey(publicKeyBytes)
        }
    }

    actual fun toSignature(encoded: List<Byte>): ECSignature {
        // TODO: There seems to be a bug with the method below, we need to wait for a fix
//        if (!Crypto.checkSignatureEncoding(encoded.toByteArray(), ScriptFlags.SCRIPT_VERIFY_DERSIG)) {
//            error("Could not parse signature from DER format")
//        }
        return ECSignature(encoded.map { it.toUByte() })
    }

    actual fun sign(text: String, privateKey: ECPrivateKey): ECSignature {
        return sign(text.encodeToByteArray().toList(), privateKey)
    }

    actual fun sign(data: List<Byte>, privateKey: ECPrivateKey): ECSignature {
        val data32 = Crypto.sha256(data.toByteArray())
        val compressedBytes = Secp256k1.sign(data32, privateKey.getEncoded().toByteArray())
        val signatureBytes = Secp256k1.compact2der(compressedBytes)

        return ECSignature(signatureBytes.toUByteArray().toList())
    }

    actual fun verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        return verify(text.encodeToByteArray().toList(), publicKey, signature)
    }

    actual fun verify(data: List<Byte>, publicKey: ECPublicKey, signature: ECSignature): Boolean {
        val data32 = Crypto.sha256(data.toByteArray())
        return Secp256k1.verify(
            signature.getEncoded().toByteArray(),
            data32,
            publicKey.getEncoded().toByteArray()
        )
    }

    actual fun isSecp256k1(point: ECPoint): Boolean =
        io.iohk.atala.prism.kotlin.crypto.Secp256k1.isSecp256k1(point)
}
