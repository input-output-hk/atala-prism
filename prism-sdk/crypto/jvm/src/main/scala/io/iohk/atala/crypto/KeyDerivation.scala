package io.iohk.atala.crypto

import java.io.ByteArrayInputStream
import java.security.SecureRandom

import io.iohk.atala.util.ArrayOps._
import org.bitcoinj.crypto.{ChildNumber, DeterministicKey, HDKeyDerivation, MnemonicCode => JMnemonicCode}
import org.bitcoinj.wallet.DeterministicSeed

import scala.collection.JavaConverters._

class GenericKeyDerivation(ec: GenericEC) extends KeyDerivationTrait {

  def instance: GenericKeyDerivation = this

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
    * Returns list of valid mnemonic words
    */
  override def getValidMnemonicWords(): Vector[String] = MnemonicCodeEnglish.WordList.toVector

  /**
    * Checks if the word is one of words used in mnemonics
    */
  override def isValidMnemonicWord(word: String): Boolean = {
    MnemonicCodeEnglish.contains(word)
  }

  /**
    * From the BIP39 spec (https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed):
    * - To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic
    * sentence (in UTF-8 NFKD) used as the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD)
    * used as the salt. The iteration count is set to 2048 and HMAC-SHA512 is used as the pseudo-random
    *   function. The length of the derived key is 512 bits (= 64 bytes).
    */
  @throws[MnemonicException]
  override def binarySeed(seed: MnemonicCode, passphrase: String): Vector[Byte] = {
    val javaWords = seed.words.asJava

    try {
      javaMnemonic.check(javaWords)
    } catch {
      case e: org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException =>
        throw new MnemonicChecksumException(e.getMessage, e)
      case e: org.bitcoinj.crypto.MnemonicException.MnemonicWordException =>
        throw new MnemonicWordException(e.getMessage, e)
      case e: org.bitcoinj.crypto.MnemonicException.MnemonicLengthException =>
        throw new MnemonicLengthException(e.getMessage, e)
      case e: Throwable =>
        throw new RuntimeException("Unexpected exception returned by MnemonicCode.check", e)
    }

    JMnemonicCode.toSeed(javaWords, passphrase).toVector
  }

  override def derivationRoot(seed: Vector[Byte]): ExtendedKey = {
    new JvmExtendedKey(HDKeyDerivation.createMasterPrivateKey(seed.toByteArray), ec)
  }

  override def deriveKey(seed: Vector[Byte], path: DerivationPath): ExtendedKey = {
    path.axes.foldLeft(derivationRoot(seed))((key, axis) => key.derive(axis))
  }
}

private[crypto] class JvmExtendedKey(key: DeterministicKey, ec: GenericEC) extends ExtendedKey {
  override def path = {
    DerivationPath(
      key.getPath.asScala.map(axis => new DerivationAxis(axis.i)).toVector
    )
  }

  override def publicKey = {
    val ecPoint = key.getPubKeyPoint
    ec.toPublicKey(ecPoint.getXCoord.toBigInteger, ecPoint.getYCoord.toBigInteger)
  }

  override def privateKey: ECPrivateKey = {
    ec.toPrivateKey(key.getPrivKey)
  }

  override def derive(axis: DerivationAxis): ExtendedKey = {
    new JvmExtendedKey(HDKeyDerivation.deriveChildKey(key, new ChildNumber(axis.i)), ec)
  }
}

object KeyDerivation extends GenericKeyDerivation(EC)

object AndroidKeyDerivation extends GenericKeyDerivation(AndroidEC)
