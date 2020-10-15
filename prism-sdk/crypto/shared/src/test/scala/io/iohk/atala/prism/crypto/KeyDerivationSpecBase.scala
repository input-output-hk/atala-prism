package io.iohk.atala.prism.crypto

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

import scala.collection.mutable

abstract class KeyDerivationSpecBase(val keyDerivation: KeyDerivationTrait) extends AnyWordSpec {

  import org.scalacheck.Arbitrary.arbitrary

  val seedGen = Gen.containerOfN[Array, Byte](64, arbitrary[Byte])
  val pathGen = Gen
    .containerOf[Vector, Int](arbitrary[Int])
    .map(axes => DerivationPath(axes.map(axis => new DerivationAxis(axis))))

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

        ECUtils.bytesToHex(binarySeed.toArray) mustBe v.binarySeedHex
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
      seed = ECUtils.hexToBytes(v.seedHex).toVector
      d <- v.derivations
    } {
      s"compute keys for seed ${v.seedHex} and path ${d.path}" in {
        val path = DerivationPath(d.path)
        val key = keyDerivation.deriveKey(seed, path)

        ECUtils.bytesToHex(key.privateKey.getEncoded) mustBe d.privKeyHex

        ECUtils.bytesToHex(key.publicKey.getEncoded) mustBe d.pubKeyHex
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
