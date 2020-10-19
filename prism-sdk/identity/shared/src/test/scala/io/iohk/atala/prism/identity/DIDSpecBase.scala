package io.iohk.atala.prism.identity

import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.identity.DID.{DIDFormat, getFormat, publicKeyToProto}
import io.iohk.atala.prism.protos.node_models
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

abstract class DIDSpecBase(val ec: ECTrait) extends AnyWordSpec {

  "DID" should {
    "create the expected long form DID" in {
      // bytes extracted from a randomly generated key
      val xBytes = Array[Byte](30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43,
        -103, 65, -63, -71, 101, 62, -46, 29, 53, -99, -22, -46, 53)
      val yBytes = Array[Byte](0, -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4,
        -116, -42, -90, -72, -118, 87, -120, 17, -119, 23, -77, -118, 69)
      val masterKey = ec.toPublicKey(xBytes, yBytes)

      // The expected resulting DID
      val expectedDID =
        "did:prism:0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77:CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

      DID.createUnpublishedDID(masterKey) mustBe expectedDID

      DID.getFormat(expectedDID) match {
        case longForm @ DIDFormat.LongForm(_, _) => longForm.validate.isEmpty mustBe false
        case _ => fail("unexpected format for long DID")
      }
    }

    val canonicalSuffix = "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
    val encodedStateUsed =
      "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

    val short = s"did:prism:$canonicalSuffix"
    val long = s"did:prism:$canonicalSuffix:$encodedStateUsed"
    val wrong = "did:prism:wrong"
    val nonPrismDID = "did:other:wrong"

    "get the correct canonical suffix" in {
      DID.getCanonicalSuffix(short).value mustBe canonicalSuffix
      DID.getCanonicalSuffix(long).value mustBe canonicalSuffix
      DID.getCanonicalSuffix(wrong) mustBe None
    }

    "tell if the DID is in canonical form" in {
      DID.isCanonicalForm(short) mustBe true
      DID.isCanonicalForm(long) mustBe false
      DID.isCanonicalForm(wrong) mustBe false
    }

    "tell if the DID is in long form" in {
      DID.isLongForm(short) mustBe false
      DID.isLongForm(long) mustBe true
      DID.isLongForm(wrong) mustBe false
    }

    "get the correct format" in {
      DID.getFormat(short) match {
        case DID.DIDFormat.Canonical(suffix) =>
          suffix mustBe canonicalSuffix
        case _ => fail("unexpected format for canonical DID")
      }
      DID.getFormat(long) match {
        case DID.DIDFormat.LongForm(stateHash, encodedState) =>
          stateHash mustBe canonicalSuffix
          encodedState mustBe encodedStateUsed
        case _ => fail("unexpected format for long DID")
      }
      DID.getFormat(wrong) match {
        case DID.DIDFormat.Unknown => // do nothing, the test would fail on the other cases
        case _ => fail("unexpected format for unknown DID")
      }
    }

    "properly obtain the DID suffix" in {
      DID.getSuffix(short).value mustBe canonicalSuffix
      DID.getSuffix(long).value mustBe s"$canonicalSuffix:$encodedStateUsed"
      DID.getSuffix(wrong) mustBe None
    }

    "properly strip the PRISM DID preffix" in {
      DID.stripPrismPrefix(short) mustBe canonicalSuffix
      DID.stripPrismPrefix(long) mustBe s"$canonicalSuffix:$encodedStateUsed"
      DID.stripPrismPrefix(nonPrismDID) mustBe nonPrismDID
    }

    "properly validate a long form DID" in {
      // bytes extracted from a randomly generated key
      val xBytes = Array[Byte](30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43,
        -103, 65, -63, -71, 101, 62, -46, 29, 53, -99, -22, -46, 53)
      val yBytes = Array[Byte](0, -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4,
        -116, -42, -90, -72, -118, 87, -120, 17, -119, 23, -77, -118, 69)
      val masterKey = ec.toPublicKey(xBytes, yBytes)

      val expectedInitialState =
        node_models.AtalaOperation(
          operation = node_models.AtalaOperation.Operation.CreateDid(
            node_models.CreateDIDOperation(
              didData = Some(
                node_models.DIDData(
                  publicKeys = Seq(
                    node_models.PublicKey(
                      id = s"master0",
                      usage = node_models.KeyUsage.MASTER_KEY,
                      keyData = node_models.PublicKey.KeyData.EcKeyData(
                        publicKeyToProto(masterKey)
                      )
                    )
                  )
                )
              )
            )
          )
        )

      getFormat(long) match {
        case validated @ DIDFormat.LongForm(_, _) =>
          validated.validate.value.initialState mustBe expectedInitialState
        case _ => fail("Long form DID with unexpected format")
      }
    }
  }

  "stripPrismPrefix" should {
    "strip the did:prism: prefix" in {
      val input = "did:prism:aabbccddee"
      val expected = "aabbccddee"
      DID.stripPrismPrefix(input) mustBe expected
    }

    "don't do anything if the exact prefix is not present" in {
      val input = "did:prism-aabbccddee"
      val expected = "did:prism-aabbccddee"
      DID.stripPrismPrefix(input) mustBe expected
    }
  }
}
