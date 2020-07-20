package io.iohk.atala.crypto

import java.io.ByteArrayInputStream
import java.security.SecureRandom

import org.bitcoinj.crypto.{ChildNumber, DeterministicKey, HDKeyDerivation, MnemonicCode => JMnemonicCode}
import org.bitcoinj.wallet.DeterministicSeed

import scala.collection.JavaConverters._

class GenericKeyDerivation(ec: GenericEC) extends KeyDerivationTrait {

  private def mnemonicCodeEnglishInputStream = {
    val wordsText = MnemonicCodeEnglish.WordList.mkString("", "\n", "\n")
    new ByteArrayInputStream(wordsText.getBytes("UTF-8"))
  }

  private lazy val javaMnemonic = new JMnemonicCode(mnemonicCodeEnglishInputStream, null)

  /**
    * Generates a random mnemonic code, usually used when a new wallet is being created.
    */
  override def randomMnemonicCode(): MnemonicCode = {
    val entropyBytes = SecureRandom.getSeed(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
    val mnemonicWords = javaMnemonic.toMnemonic(entropyBytes)

    MnemonicCode(mnemonicWords.asScala.toList)
  }

  /**
    * From the BIP39 spec (https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed):
    * - To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic
    * sentence (in UTF-8 NFKD) used as the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD)
    * used as the salt. The iteration count is set to 2048 and HMAC-SHA512 is used as the pseudo-random
    *   function. The length of the derived key is 512 bits (= 64 bytes).
    */
  override def binarySeed(seed: MnemonicCode, passphrase: String): Vector[Byte] = {
    JMnemonicCode.toSeed(seed.words.asJava, passphrase).toVector
  }

  override def derivationRoot(seed: Vector[Byte]): ExtendedKey = {
    new JvmExtendedKey(HDKeyDerivation.createMasterPrivateKey(seed.toArray), ec)
  }

  override def deriveKey(seed: Vector[Byte], path: DerivationPath): ExtendedKey = {
    path.axes.foldLeft(derivationRoot(seed))((key, axis) => key.derive(axis))
  }
}

private[crypto] class JvmExtendedKey(key: DeterministicKey, ec: GenericEC) extends ExtendedKey {
  override def path = {
    DerivationPath(
      key.getPath.asScala.map(_.i).toVector
    )
  }

  override def publicKey = {
    val ecPoint = key.getPubKeyPoint
    ec.toPublicKey(ecPoint.getXCoord.toBigInteger, ecPoint.getYCoord.toBigInteger)
  }

  override def privateKey: ECPrivateKey = {
    ec.toPrivateKey(key.getPrivKey)
  }

  override def derive(axis: Int): ExtendedKey = {
    new JvmExtendedKey(HDKeyDerivation.deriveChildKey(key, new ChildNumber(axis)), ec)
  }

  override def deriveHardened(axis: Int): ExtendedKey = {
    derive(axis | ChildNumber.HARDENED_BIT)
  }
}

object KeyDerivation extends GenericKeyDerivation(EC)

object AndroidKeyDerivation extends GenericKeyDerivation(AndroidEC)
