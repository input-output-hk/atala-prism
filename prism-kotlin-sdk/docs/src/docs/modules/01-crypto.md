# Crypto

Add this code to your `build.gradle` to install the Crypto module:

```kotlin
implementation("io.iohk.atala.prism:crypto:$VERSION")
```

## SHA256 hash

The Crypto module provides a [SHA256](https://en.wikipedia.org/wiki/SHA-2) hash function that can be used as follows:

```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest

SHA256Digest.compute(byteArrayOf(0))
```

## Elliptic-curve cryptography

This module includes low-level tools to work with public-key cryptography based on [elliptic curves](https://en.wikipedia.org/wiki/Elliptic-curve_cryptography).

Some helpful globals can be found in `ECConfig`:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.ECConfig

ECConfig.CURVE_NAME
ECConfig.PRIVATE_KEY_BYTE_SIZE
ECConfig.PUBLIC_KEY_BYTE_SIZE
ECConfig.SIGNATURE_ALGORITHM
```

`EC` provides access to key generation, signing and verification:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.EC

val keyPair = EC.generateKeyPair()
val privateKey = keyPair.privateKey
val publicKey = keyPair.publicKey

val data = byteArrayOf(1)
val dataSignature = EC.sign(data, privateKey)
EC.verify(data, publicKey, dataSignature)

val text = "PRISM"
val textSignature = EC.sign(text, privateKey)
EC.verify(text, publicKey, textSignature)
```

Keys can also be encoded and decoded as follows:
```kotlin:ank
val privateKeyEncoded = privateKey.getEncoded()
val publicKeyEncoded = publicKey.getEncoded()

EC.toPrivateKey(privateKeyEncoded) == privateKey
EC.toPublicKey(publicKeyEncoded) == publicKey
```

## Key Derivation

You can use this module to derive keys from a given seed.

First, there are some utilities to work with mnemonic codes:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyDerivation

val mnemonicCode = KeyDerivation.randomMnemonicCode()
// or use the following code to use your already existing seed phrase (list of 12 words)
// val mySeedPhrase = listOf() <- provide your list of 12 words here as strings
// val mnemonicCode = MnemonicCode(mySeedPhrase)
KeyDerivation.getValidMnemonicWords().take(10)
KeyDerivation.isValidMnemonicWord("airport")
```

After obtaining a mnemonic code, you can generate a mnemonic seed and a root key:
```kotlin:ank
val seed = KeyDerivation.binarySeed(mnemonicCode, "my_secret_password")
val extendedKey = KeyDerivation.derivationRoot(seed)

// An extended key contains the private and public keys inside as well as the
// derivation path used to obtain the key
extendedKey.keyPair()
extendedKey.privateKey()
extendedKey.publicKey()
extendedKey.path()
```

This module also supports derivation paths, so you can derive children keys:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.derivation.DerivationPath

val path = DerivationPath.fromPath("m/0'/0'/1'")
KeyDerivation.deriveKey(seed, path)
```

## Merkle Tree

It is possible to build and verify [Merkle tree](https://en.wikipedia.org/wiki/Merkle_tree) proofs of inclusion by using `MerkleTree`:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.*

val hash1 = SHA256Digest.compute(byteArrayOf(0))
val hash2 = SHA256Digest.compute(byteArrayOf(1))

val (root, proofs) = generateProofs(listOf(hash1, hash2))
val (proof1, proof2) = proofs

verifyProof(root, proof1)
verifyProof(root, proof2)
```
