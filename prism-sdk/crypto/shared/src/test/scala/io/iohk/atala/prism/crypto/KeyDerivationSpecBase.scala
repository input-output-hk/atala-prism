package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.BytesOps
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

import scala.collection.mutable
import javax.crypto._
import javax.crypto.spec._
import java.security._
import java.nio.charset.StandardCharsets
import java.util.Base64

abstract class KeyDerivationSpecBase(val keyDerivation: KeyDerivationTrait) extends AnyWordSpec {

  import org.scalacheck.Arbitrary.arbitrary

  val seedGen = Gen.containerOfN[Array, Byte](64, arbitrary[Byte])
  val pathGen = Gen
    .containerOf[Vector, Int](arbitrary[Int])
    .map(axes => DerivationPath(axes.map(axis => new DerivationAxis(axis))))

  private val encryptionAlgorithm = "AES/CBC/PKCS5Padding"
  private val keyGenerationAlgorithm = "AES"

  private def md5(bytes: Array[Byte]): Array[Byte] = {
    val md5Hash = MessageDigest.getInstance("MD5")
    md5Hash.update(bytes)
    md5Hash.digest
  }

  private def AESEncrypt(data: Array[Byte], key: SecretKey, iv: IvParameterSpec): Array[Byte] = {
    val cipher = Cipher.getInstance(encryptionAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    cipher.doFinal(data)
  }

  private def AESDecrypt(data: Array[Byte], key: SecretKey, iv: IvParameterSpec): Array[Byte] = {
    val cipher = Cipher.getInstance(encryptionAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    cipher.doFinal(data)
  }

  "Proof of concept" should {
    "Derive private and public keys from mnemonic and use then as AES key and IV" in {
      val mnemonic = keyDerivation.randomMnemonicCode()
      val initialSeed = keyDerivation.binarySeed(mnemonic, BIP39TestVectors.password)
      // I'll generate an extended key with some arbitrary path m/1'/2'
      val extendedKey = keyDerivation.deriveKey(
        initialSeed,
        DerivationPath(
          Vector(
            DerivationAxis.hardened(1),
            DerivationAxis.hardened(2)
          )
        )
      )

      // got public and private keys (as byte arrays)
      val ecPrivateKeyBytes = extendedKey.privateKey.getEncoded
      val ecPublicKeyBytes = extendedKey.publicKey.getEncoded

      /**
        * Here I'm just using EC private key as a Secret Key for AES algorithm, since EC
        * secret key is just a big integer, and AES secret key can be any sequence of bytes
        * that is either 128, 192, or 256 bits long, EC private can be used as AES secret key
        * assuming it has been derived from sufficient amount of entropy
        */
      val key = new SecretKeySpec(ecPrivateKeyBytes, keyGenerationAlgorithm)

      /**
        * Initialization vector does not have any special requirements other then that it
        * has to be 128 bits long, here I'm using the public key as an IV since it can be deterministically
        * derived from the mnemonic and derivation path, it is easy to associate IV to the key since it is
        * a public key of its private key. In this case this key is not really a "key", it is just some random bytes
        * that can be deterministically derived, and it is not really "public", as leaking this key to the outside world
        * would be a problem. I'm using md5 to make it 128 bits, I could have just taken first 128 bits as well since it
        * is 65 bytes long.
        */
      val iv = new IvParameterSpec(md5(ecPublicKeyBytes))

      val secretMessage = "Connector message that should be encrypted"
      println(s"The original message is: ${secretMessage}")

      val cipherTextBytes = AESEncrypt(secretMessage.getBytes, key, iv)
      val cipherTextStr = new String(cipherTextBytes, StandardCharsets.UTF_8)
      println(s"Encrypted text is: ${cipherTextStr}")

      val decryptedTextBytes = AESDecrypt(cipherTextBytes, key, iv)
      val decryptedTextStr = new String(decryptedTextBytes, StandardCharsets.UTF_8)
      println(s"Decrypted text is: ${decryptedTextStr}")

      secretMessage mustBe decryptedTextStr

    }

    "derive the key and decrypt encrypted message from seed and derivation path" in {
      // only knowing word list and key index path, I should be able to derive the key
      // and decrypt messages that have been encrypted with that key using AES

      val originalText = "Connector message that should be encrypted"
      val base64EncodedCypherText = "yr5HBjwFcIZeoRLOaeHQmENiJIvPZEfaYozHfzUUYxeK1YnrJIxUb4HUYk6Ouv1l"
      val wordList = List(
        "option",
        "width",
        "fitness",
        "weasel",
        "luxury",
        "picture",
        "question",
        "subject",
        "prize",
        "chaos",
        "twelve",
        "dove"
      )

      val indexPath = "m/1'/2'"

      val mnemonicCode = MnemonicCode(wordList)
      val initialSeed = keyDerivation.binarySeed(mnemonicCode, BIP39TestVectors.password)
      val derivationPath = DerivationPath(indexPath)

      val extendedKey = keyDerivation.deriveKey(initialSeed, derivationPath)
      val ecPrivateKeyBytes = extendedKey.privateKey.getEncoded
      val ecPublicKeyBytes = extendedKey.publicKey.getEncoded

      val key = new SecretKeySpec(ecPrivateKeyBytes, keyGenerationAlgorithm)
      val iv = new IvParameterSpec(md5(ecPublicKeyBytes))

      val decoded = AESDecrypt(Base64.getDecoder.decode(base64EncodedCypherText), key, iv)
      val stringDecoded = new String(decoded, StandardCharsets.UTF_8)

      stringDecoded mustBe originalText

    }
  }

  "randomMnemonicCode" should {
    "generate 12-word mnemonics" in {
      for (_ <- 1 to 10) {
        keyDerivation.randomMnemonicCode().words.length mustBe 12
      }
    }

    "generate random mnemonics" in {
      val seenWords = mutable.Set.empty[String]
      for (_ <- 1 to 300; word <- keyDerivation.randomMnemonicCode().words) {
        seenWords.add(word)
      }

      // with great probability we'll see at least 75% of words after 3600 draws from 2048 possible
      2048 - seenWords.size must be < 512
    }
  }

  "isValidMnemonicWord" should {
    "return true for mnemonic words" in {
      for (word <- keyDerivation.getValidMnemonicWords()) {
        keyDerivation.isValidMnemonicWord(word) mustBe true
      }
    }

    "return false for invalid words" in {
      keyDerivation.isValidMnemonicWord("hocus") mustBe false
    }
  }

  "binarySeed" should {
    for (v <- BIP39TestVectors.testVectors) {
      s"compute right binary seed for mnemonic code ${v.entropyHex}" in {
        val mnemonicCode = MnemonicCode(v.mnemonicPhrase.split(" ").toList)
        val binarySeed = keyDerivation.binarySeed(mnemonicCode, BIP39TestVectors.password)

        BytesOps.bytesToHex(binarySeed) mustBe v.binarySeedHex
      }
    }

    "fail when checksum is not correct" in {
      val mnemonicCode = MnemonicCode(List.fill(15)("abandon"))
      intercept[MnemonicChecksumException] {
        keyDerivation.binarySeed(mnemonicCode, "")
      }
    }

    "fail when invalid word is used" in {
      val mnemonicCode = MnemonicCode(List("hocus", "pocus", "mnemo", "codus") ++ List.fill(11)("abandon"))
      intercept[MnemonicWordException] {
        keyDerivation.binarySeed(mnemonicCode, "")
      }
    }

    "fail when mnemonic code has wrong length" in {
      val mnemonicCode = MnemonicCode(List("abandon"))
      intercept[MnemonicLengthException] {
        keyDerivation.binarySeed(mnemonicCode, "")
      }
    }
  }

  "deriveKey" should {
    for {
      v <- BIP32TestVectors.testVectors
      seed = BytesOps.hexToBytes(v.seedHex).toVector
      d <- v.derivations
    } {
      s"compute keys for seed ${v.seedHex} and path ${d.path}" in {
        val path = DerivationPath(d.path)
        val key = keyDerivation.deriveKey(seed, path)

        BytesOps.bytesToHex(key.privateKey.getEncoded) mustBe d.privKeyHex

        BytesOps.bytesToHex(key.publicKey.getEncoded) mustBe d.pubKeyHex
      }
    }
  }

  "deriveKey" should {
    "return the same result as manual derivation from key" in {
      forAll(seedGen, arbitrary[Seq[Int]]) { (seed, axes) =>
        val path = DerivationPath(axes.toVector.map(new DerivationAxis(_)))
        val rootKey = keyDerivation.derivationRoot(seed.toVector)
        val manualKey = axes.foldLeft(rootKey)((key, axis) => key.derive(new DerivationAxis(axis)))
        val obtainedKey = keyDerivation.deriveKey(seed.toVector, path)
        obtainedKey.path mustBe manualKey.path
        obtainedKey.privateKey.getEncoded must contain theSameElementsInOrderAs manualKey.privateKey.getEncoded
      }
    }
  }

  "DerivationPath" should {
    "properly apply derivations" in {
      forAll(pathGen) { path =>
        path.axes.foldLeft(DerivationPath())((path, axis) => path.derive(axis)) mustBe path
      }
    }

    "parse string representations of path" in {
      forAll(pathGen) { path =>
        DerivationPath(path.toString).axes must contain theSameElementsInOrderAs path.axes
      }
    }
  }
}
