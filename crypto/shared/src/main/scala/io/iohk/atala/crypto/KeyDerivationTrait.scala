package io.iohk.atala.crypto

/**
  * Represents a mnemonic code, word by word.
  */
case class MnemonicCode(words: List[String])

/** Represents derivation path in BIP 32 protocol */
case class DerivationPath(axes: Vector[Int]) {

  private def axisToString(axis: Int): String = {
    if (((axis >> 31) & 1) == 1) {
      (axis & ~(1 << 31)).toString + "'"
    } else {
      axis.toString
    }
  }

  /** Creates child derivation path for given index, hardened or not */
  def derive(axis: Int): DerivationPath = {
    copy(axes = axes :+ axis)
  }

  /** Creates child derivation path for hardened child
    *
    * @param axis number of hardened child, 2^31^ is added to obtain the index
    */
  def deriveHardened(axis: Int): DerivationPath = {
    copy(axes = axes :+ (axis | (1 << 31)))
  }

  override def toString = ("m" +: axes.map(axisToString)).mkString("/")
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
      DerivationPath(splitPath.tail.map(parseAxis).toVector)
    }
  }

  /** Computes hardened index for given number */
  def harden(i: Int): Int = i | (1 << 31)

  private def parseAxis(axis: String): Int = {
    val hardened = axis.endsWith("'")
    val axisNumStr = if (hardened) axis.substring(0, axis.length - 1) else axis
    val axisNum = Integer.parseInt(axisNumStr)
    if (hardened) harden(axisNum) else axisNum
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
  def derive(axis: Int): ExtendedKey

  /** Generates child extended key for hardened index */
  def deriveHardened(axis: Int): ExtendedKey
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
