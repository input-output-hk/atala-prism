package io.iohk.atala.crypto

import java.util.Base64

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

import scala.collection.mutable

abstract class KeyDerivationSpecBase(val keyDerivation: KeyDerivationTrait) extends AnyWordSpec {

  import org.scalacheck.Arbitrary.arbitrary

  val seedGen = Gen.containerOfN[Array, Byte](64, arbitrary[Byte])

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

  "binarySeed" should {
    for (v <- BIP39TestVectors.testVectors) {
      s"compute right binary seed for mnemonic code ${v.entropyHex}" in {
        val mnemonicCode = MnemonicCode(v.mnemonicPhrase.split(" ").toList)
        val binarySeed = keyDerivation.binarySeed(mnemonicCode, BIP39TestVectors.password)

        ECUtils.bytesToHex(binarySeed.toArray) mustBe v.binarySeedHex
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
        val path = DerivationPath(axes.toVector)
        val rootKey = keyDerivation.derivationRoot(seed.toVector)
        val manualKey = axes.foldLeft(rootKey)((key, axis) => key.derive(axis))
        val obtainedKey = keyDerivation.deriveKey(seed.toVector, path)
        obtainedKey.path mustBe manualKey.path
        obtainedKey.privateKey.getEncoded must contain theSameElementsInOrderAs manualKey.privateKey.getEncoded
      }
    }
  }

  "DerivationPath" should {
    "properly apply derivations" in {
      forAll { axes: Vector[Int] =>
        axes.foldLeft(DerivationPath())((path, axis) => path.derive(axis)) mustBe DerivationPath(axes)
      }
    }

    "parse string representations of path" in {
      forAll { axes: Vector[Int] =>
        DerivationPath(DerivationPath(axes).toString).axes must contain theSameElementsInOrderAs axes
      }
    }
  }
}
