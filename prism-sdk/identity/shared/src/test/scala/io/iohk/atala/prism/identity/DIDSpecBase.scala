package io.iohk.atala.prism.identity

import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.identity.DID.{DIDFormat, publicKeyToProto}
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
      val expectedDID = DID.buildPrismDID(
        "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77",
        "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"
      )

      DID.createUnpublishedDID(masterKey) mustBe expectedDID

      expectedDID.getFormat match {
        case longForm @ DIDFormat.LongForm(_, _) => longForm.validate.isLeft mustBe false
        case _ => fail("unexpected format for long DID")
      }
    }

    val canonicalSuffix = "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
    val encodedStateUsed =
      "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

    val short = DID.buildPrismDID(canonicalSuffix)
    val long = DID.buildPrismDID(canonicalSuffix, encodedStateUsed)
    val wrong = DID.buildPrismDID("wrong")

    "get the correct canonical suffix" in {
      short.getCanonicalSuffix.value mustBe canonicalSuffix
      long.getCanonicalSuffix.value mustBe canonicalSuffix
      wrong.getCanonicalSuffix mustBe None
    }

    "tell if the DID is in canonical form" in {
      short.isCanonicalForm mustBe true
      long.isCanonicalForm mustBe false
      wrong.isCanonicalForm mustBe false
    }

    "tell if the DID is in long form" in {
      short.isLongForm mustBe false
      long.isLongForm mustBe true
      wrong.isLongForm mustBe false
    }

    "get the correct format" in {
      short.getFormat match {
        case DID.DIDFormat.Canonical(suffix) =>
          suffix mustBe canonicalSuffix
        case _ => fail("unexpected format for canonical DID")
      }
      long.getFormat match {
        case DID.DIDFormat.LongForm(stateHash, encodedState) =>
          stateHash mustBe canonicalSuffix
          encodedState mustBe encodedStateUsed
        case _ => fail("unexpected format for long DID")
      }
      wrong.getFormat match {
        case DID.DIDFormat.Unknown => // do nothing, the test would fail on the other cases
        case _ => fail("unexpected format for unknown DID")
      }
    }

    "properly obtain the DID suffix" in {
      short.getSuffix.value mustBe canonicalSuffix
      long.getSuffix.value mustBe s"$canonicalSuffix:$encodedStateUsed"
      wrong.getSuffix mustBe None
    }

    "properly strip the PRISM DID preffix" in {
      short.stripPrismPrefix mustBe canonicalSuffix
      long.stripPrismPrefix mustBe s"$canonicalSuffix:$encodedStateUsed"
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

      long.getFormat match {
        case validated @ DIDFormat.LongForm(_, _) =>
          validated.validate.toOption.value.initialState mustBe expectedInitialState
        case _ => fail("Long form DID with unexpected format")
      }
    }

    "be creatable from test prefix" in {
      val rawDid = "did:test:int-demo"
      val did = DID.fromString(rawDid)
      did.value.value mustBe rawDid
    }
  }

  "stripPrismPrefix" should {
    "strip the did:prism: prefix" in {
      val input = DID.buildPrismDID("aabbccddee")
      val expected = "aabbccddee"
      input.stripPrismPrefix mustBe expected
    }
  }

  "unsafeFromString" should {
    "succeed for valid DID" in {
      val validDid = DID.buildPrismDID("aabbccddee")

      val unsafeDid = DID.unsafeFromString(validDid.value)

      unsafeDid mustBe validDid
    }

    "fail for invalid DID" in {
      val caught =
        intercept[IllegalArgumentException] {
          DID.unsafeFromString("invalid-did")
        }
      caught.getMessage mustBe "Invalid DID invalid-did"
    }
  }
}
