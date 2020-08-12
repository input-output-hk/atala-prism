package io.iohk.atala.crypto.japi

import io.iohk.atala.util.ArrayOps._

import scala.collection.JavaConverters._

class ExtendedKeyFacade(key: io.iohk.atala.crypto.ExtendedKey) extends ExtendedKey {
  override def getPath(): DerivationPath = {
    new DerivationPath(seqAsJavaList(key.path.axes.map(axis => DerivationAxis.raw(axis.i))))
  }

  override def getPublic(): ECPublicKey = {
    new ECPublicKeyFacade(key.publicKey)
  }

  override def getPrivate(): ECPrivateKey = {
    new ECPrivateKeyFacade(key.privateKey)
  }

  override def getKeyPair(): ECKeyPair = {
    new ECKeyPairFacade(key.keyPair)
  }

  override def derive(axis: DerivationAxis): ExtendedKey = {
    new ExtendedKeyFacade(key.derive(new io.iohk.atala.crypto.DerivationAxis(axis.getI)))
  }
}

class KeyDerivationFacade(keyDerivation: io.iohk.atala.crypto.KeyDerivationTrait) extends KeyDerivation {
  override def randomMnemonicCode(): MnemonicCode = {
    new MnemonicCode(keyDerivation.randomMnemonicCode().words.asJava)
  }

  override def isValidMnemonicWord(word: String): Boolean = keyDerivation.isValidMnemonicWord(word)

  override def getValidMnemonicWords: java.util.List[String] = keyDerivation.getValidMnemonicWords().asJava

  override def binarySeed(seed: MnemonicCode, passphrase: String): Array[Byte] = {
    keyDerivation
      .binarySeed(
        io.iohk.atala.crypto.MnemonicCode.apply(seed.getWords.asScala.toList),
        passphrase
      )
      .toByteArray
  }

  override def derivationRoot(seed: Array[Byte]): ExtendedKey = {
    new ExtendedKeyFacade(keyDerivation.derivationRoot(seed.toVector))
  }

  override def deriveKey(seed: Array[Byte], path: DerivationPath): ExtendedKey = {
    new ExtendedKeyFacade(
      keyDerivation.deriveKey(
        seed.toVector,
        io.iohk.atala.crypto
          .DerivationPath(path.getAxes.asScala.map(axis => new io.iohk.atala.crypto.DerivationAxis(axis.getI)).toVector)
      )
    )
  }
}
