package io.iohk.atala.crypto

/**
  * Represents a mnemonic code, word by word.
  */
case class MnemonicCode(words: List[String])

/** Represent an axis on BIP 32 key derivation path
  *
  * BIP 32 standard defines how keys can be derived from another one for index between 0 and 2^32^ - 1, where
  * indices between 0 and 2^31^ - 1 are called normal indices and between 2^31^ and 2^32^ - 1 hardened indices.
  * Natural way to represent such thing is unsigned 32-bit integer, but JVM (and Scala) doesn't support such
  * data type. That is why signed 32-bit is used instead, with the same bit representation. In other words
  * unsigned index used here is equivalent to canonical, unsigned one modulo 2^32^.
  *
  * Implementation details are mostly hidden from the user, so user can either choose to create a normal
  * axis, providing number between 0 and 2^31^ - 1 or hardened one, providing a number from the same range.
  */
class DerivationAxis private[crypto] (val i: Int) extends AnyVal {

  /** Checks if the axis is hardened */
  def isHardened: Boolean = ((i >> 31) & 1) == 1

  /** Returns number corresponding to the axis (different for index), always between 0 and 2^31^ */
  def number: Int = i & ~(1 << 31)

  /** Renders axis as number with optional ' for hardened path, e.g. 1 or 7' */
  def axisToString(): String = {
    if (isHardened) {
      number.toString + "'"
    } else {
      i.toString
    }
  }
}

object DerivationAxis {

  /** Creates normal (non-hardened) axis
    * @param num number corresponding to the axis, must be between 0 and 2^31^ - 1
    */
  def normal(num: Int): DerivationAxis = {
    require(num >= 0)
    new DerivationAxis(num)
  }

  /** Creates hardened axis
    * @param num number corresponding to the axis, must be between 0 and 2^31^ - 1
    */
  def hardened(num: Int): DerivationAxis = {
    require(num >= 0)
    new DerivationAxis(num | (1 << 31))
  }
}

/** Represents derivation path in BIP 32 protocol */
case class DerivationPath(axes: Vector[DerivationAxis]) {

  /** Creates child derivation path for given index, hardened or not */
  def derive(axis: DerivationAxis): DerivationPath = {
    copy(axes = axes :+ axis)
  }

  override def toString = ("m" +: axes.map(_.axisToString())).mkString("/")
}

object DerivationPath {

  /** Constructs empty derivation path */
  def apply(): DerivationPath = DerivationPath(Vector.empty)

  /** Parses string representation of derivation path

    * @param path Path to parse in format m/axis1/axis2/.../axisn where all axes are number between 0 and 2^31^ - 1 and
    *             optionally a ' added after to mark hardened axis e.g. m/21/37'/0
    */
  def apply(path: String): DerivationPath = {
    val splitPath = path.split("/")
    if (!splitPath.headOption.map(_.trim.toLowerCase).contains("m")) {
      throw new IllegalArgumentException("Path needs to start with m or M")
    } else {
      DerivationPath(splitPath.tail.toVector.map(parseAxis))
    }
  }

  private def parseAxis(axis: String): DerivationAxis = {
    val hardened = axis.endsWith("'")
    val axisNumStr = if (hardened) axis.substring(0, axis.length - 1) else axis
    val axisNum = Integer.parseInt(axisNumStr)
    if (hardened) DerivationAxis.hardened(axisNum) else DerivationAxis.normal(axisNum)
  }

}

trait ExtendedKey {

  /** Derivation path used to obtain such key */
  def path: DerivationPath

  /** Public key for this extended key */
  def publicKey: ECPublicKey

  /** Private key for this extended key */
  def privateKey: ECPrivateKey

  /** KeyPair for this extended key */
  def keyPair: ECKeyPair = ECKeyPair(privateKey, publicKey)

  /** Generates child extended key for given index */
  def derive(axis: DerivationAxis): ExtendedKey
}

/**
  * These methods should be enough to implement our key derivation strategy:
  * - https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
  *
  * The goal is to be able to use it on the Android app, and on the Browser Wallet.
  */
trait KeyDerivationTrait {

  /**
    * Generates a random mnemonic code, usually used when a new wallet is being created.
    */
  def randomMnemonicCode(): MnemonicCode

  /** Checks if the word is one of words used in mnemonics */
  def isValidMnemonicWord(word: String): Boolean

  /** Returns list of valid mnemonic words */
  def getValidMnemonicWords(): Vector[String]

  /**
    * From the BIP39 spec (https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed):
    * - To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic
    *   sentence (in UTF-8 NFKD) used as the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD)
    *   used as the salt. The iteration count is set to 2048 and HMAC-SHA512 is used as the pseudo-random
    *   function. The length of the derived key is 512 bits (= 64 bytes).
    */
  def binarySeed(seed: MnemonicCode, passphrase: String): Vector[Byte]

  /** Computes master key from seed bytes, according to BIP 32 protocol*/
  def derivationRoot(seed: Vector[Byte]): ExtendedKey

  /** Computes key in derivation tree from seed bytes, according to BIP 32 protocol*/
  def deriveKey(seed: Vector[Byte], path: DerivationPath): ExtendedKey = {
    path.axes.foldLeft(derivationRoot(seed))((key, axis) => key.derive(axis))
  }

}
