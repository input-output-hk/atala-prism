# Atala PRISM SDK - Crypto

To install the module:
```scala
libraryDependencies += "io.iohk" % "prism-crypto" % "@VERSION@"
```

## SHA256 Hash

Atala PRISM Crypto module provides SHA256 hash function that can be used as follows:
```scala mdoc:to-string
import io.iohk.atala.prism.crypto.SHA256Digest

SHA256Digest.compute(Array(0.toByte))
```

## Elliptic-curve Cryptography

Crypto module contains low-level tools to work with public-key cryptography based on elliptic curves.

Some helpful globals can be found in `ECConfig`:
```scala mdoc
import io.iohk.atala.prism.crypto.ECConfig

ECConfig.CURVE_NAME

ECConfig.CURVE_FIELD_BYTE_SIZE

ECConfig.SIGNATURE_ALGORITHM
```

`EC` provides access to key generation, signing and verification:
```scala mdoc:to-string
import io.iohk.atala.prism.crypto.EC

val keyPair = EC.generateKeyPair()
val privateKey = keyPair.privateKey
val publicKey = keyPair.publicKey

val data = Array(1.toByte)
val dataSignature = EC.sign(data, privateKey)
EC.verify(data, publicKey, dataSignature)

val text = "PRISM"
val textSignature = EC.sign(text, privateKey)
EC.verify(text, publicKey, textSignature)
```

Keys can also be encoded and decoded as follows:
```scala mdoc:to-string
val privateKeyEncoded = privateKey.getEncoded
val publicKeyEncoded = publicKey.getEncoded

EC.toPrivateKey(privateKeyEncoded) == privateKey
EC.toPublicKey(publicKeyEncoded) == publicKey
```

## Key Derivation

Atala PRISM Crypto module provides the means to derive keys from a given seed.

First, there are some utilities to work with mnemonic codes:
```scala mdoc
import io.iohk.atala.prism.crypto.KeyDerivation

val mnemonicCode = KeyDerivation.randomMnemonicCode()
KeyDerivation.getValidMnemonicWords().take(10)
KeyDerivation.isValidMnemonicWord("airport")
```

After you have a mnemonic code, you can generate a mnemonic seed and, subsequently, a root key:
```scala mdoc:to-string
val seed = KeyDerivation.binarySeed(mnemonicCode, "my_secret_password")
val extendedKey = KeyDerivation.derivationRoot(seed)

// An extended key contains the private and public keys inside as well as the
// derivation path used to obtain the key
extendedKey.keyPair
extendedKey.privateKey
extendedKey.publicKey
extendedKey.path
```

Crypto module also supports derivation paths, so you can derive children keys as follows:
```scala mdoc:to-string
import io.iohk.atala.prism.crypto.DerivationPath

val path = DerivationPath("m/0'/0'/1'")
KeyDerivation.deriveKey(seed, path)
```

## Merkle Tree

It is possible to build Merkle tree proofs of inclusion and verify them by using `MerkleTree`:
```scala mdoc:to-string
import cats.data.NonEmptyList
import io.iohk.atala.prism.crypto.MerkleTree

val hash1 = SHA256Digest.compute(Array(0.toByte))
val hash2 = SHA256Digest.compute(Array(1.toByte))

val (root, List(proof1, proof2)) = MerkleTree.generateProofs(NonEmptyList.of(hash1, hash2))

MerkleTree.verifyProof(root, proof1)
MerkleTree.verifyProof(root, proof2)
```
