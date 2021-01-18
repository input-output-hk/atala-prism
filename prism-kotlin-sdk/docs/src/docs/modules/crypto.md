To install the module add the following to your `build.gradle`:
```kotlin
implementation("io.iohk.atala.prism:crypto:$VERSION")
```

## SHA256 Hash

Atala PRISM Crypto module provides SHA256 hash function that can be used as follows:

```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest

SHA256Digest.compute(listOf(0))
```

## Elliptic-curve Cryptography

Crypto module contains low-level tools to work with public-key cryptography based on elliptic curves.

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

val data = listOf<Byte>(1)
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

Atala PRISM Crypto module provides the means to derive keys from a given seed.

First, there are some utilities to work with mnemonic codes:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.kotlin.crypto.derivation.JvmKeyDerivation

val keyDerivation: KeyDerivation = JvmKeyDerivation // Pick the appropriate one for your platform (e.g. JvmKeyDerivation for JVM)
val mnemonicCode = keyDerivation.randomMnemonicCode()
keyDerivation.getValidMnemonicWords().take(10)
keyDerivation.isValidMnemonicWord("airport")
```

After you have a mnemonic code, you can generate a mnemonic seed and, subsequently, a root key:
```kotlin:ank
val seed = keyDerivation.binarySeed(mnemonicCode, "my_secret_password")
val extendedKey = keyDerivation.derivationRoot(seed)

// An extended key contains the private and public keys inside as well as the
// derivation path used to obtain the key
extendedKey.keyPair()
extendedKey.privateKey()
extendedKey.publicKey()
extendedKey.path()
```

Crypto module also supports derivation paths, so you can derive children keys as follows:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.derivation.DerivationPath

val path = DerivationPath.fromPath("m/0'/0'/1'")
keyDerivation.deriveKey(seed, path)
```

## Merkle Tree

It is possible to build Merkle tree proofs of inclusion and verify them by using `MerkleTree`:
```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.MerkleTree

val hash1 = SHA256Digest.compute(listOf(0))
val hash2 = SHA256Digest.compute(listOf(1))

val (root, proofs) = MerkleTree.generateProofs(listOf(hash1, hash2))
val (proof1, proof2) = proofs

MerkleTree.verifyProof(root, proof1)
MerkleTree.verifyProof(root, proof2)
```
