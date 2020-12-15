package io.iohk.atala.prism.kotlin.crypto

import cocoapods.bitcoin_secp256k1.SECP256K1_CONTEXT_SIGN
import cocoapods.bitcoin_secp256k1.SECP256K1_CONTEXT_VERIFY
import cocoapods.bitcoin_secp256k1.secp256k1_context_create
import cocoapods.bitcoin_secp256k1.secp256k1_ec_pubkey_create
import cocoapods.bitcoin_secp256k1.secp256k1_pubkey
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
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
        fread(privateKeyPtr, 1.convert(), ECConfig.PRIVATE_KEY_BYTE_SIZE.convert(), urandom)
        for (n in 0 until ECConfig.PRIVATE_KEY_BYTE_SIZE) privateKey[n] = privateKeyPtr[n]
        fclose(urandom)
        return privateKey
    }
    
    @ExperimentalUnsignedTypes
    actual fun generateKeyPair(): ECKeyPair {
        return memScoped { 
            val context = secp256k1_context_create((SECP256K1_CONTEXT_SIGN or SECP256K1_CONTEXT_VERIFY).convert())
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
}
