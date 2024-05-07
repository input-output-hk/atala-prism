package io.iohk.atala.prism.node.identity

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.utils.Base64Utils
import io.iohk.atala.prism.node.crypto.{CryptoUtils, CryptoTestUtils}
import io.iohk.atala.prism.protos.node_models._
import org.scalatest.OptionValues._

class PrismDidSpec extends AnyWordSpec {

  val canonicalSuffixHex = "9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b"
  val canonicalSuffix = Sha256Hash.fromHex(canonicalSuffixHex)
  val encodedStateUsedBase64 =
    "Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDHpf-yhIns-LP3tLvA8icC5FJ1ZlBwbllPtIdNZ3q0jU"
  val encodedStateUsed = Base64Utils.decodeURL(encodedStateUsedBase64)

  val short = PrismDid.buildCanonical(canonicalSuffix)
  val long = PrismDid.buildLongForm(canonicalSuffix, encodedStateUsed)

  private def createLongFormDidFromAtalaOperationUnsafely(atalaOperation: AtalaOperation): LongFormPrismDid = {
    val encodedState = atalaOperation.toByteArray
    val encodedStateBase64 = Base64Utils.encodeURL(encodedState)
    val stateHash = Sha256Hash.compute(encodedState)

    val methodSpecificId = DidMethodSpecificId.fromSections(Array(stateHash.hexEncoded, encodedStateBase64))
    val did = Did(PrismDid.PRISM_METHOD, methodSpecificId)
    LongFormPrismDid(did, stateHash, atalaOperation)
  }

  "PrismDid library" should {

    // HASHING
    "asCanonical should work for long and short form dids" in {
      canonicalSuffixHex mustBe short.asCanonical().suffix
      canonicalSuffixHex mustBe long.asCanonical().suffix
    }

    "values of canonical did should be a valid prism did with correct suffix" in {
      short.asCanonical().value mustBe s"did:prism:$canonicalSuffixHex"
      long.asCanonical().value mustBe s"did:prism:$canonicalSuffixHex"
    }

    "obtain did suffix correctly" in {
      short.suffix mustBe canonicalSuffixHex
      long.suffix mustBe s"$canonicalSuffixHex:$encodedStateUsedBase64"
    }

    "properly validate a long for DID" in {
      // bytes extracted from a randomly generated key
      val xBytes = Array[Byte](
        30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43, -103, 65, -63, -71,
        101, 62, -46, 29, 53, -99, -22, -46, 53
      )
      val yBytes = Array[Byte](
        -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4, -116, -42, -90, -72, -118,
        87, -120, 17, -119, 23, -77, -118, 69
      )
      val masterKey = CryptoUtils.SecpPublicKey.unsafeFromByteCoordinates(xBytes, yBytes)

      val expectedInitialState =
        AtalaOperation(
          operation = AtalaOperation.Operation.CreateDid(
            CreateDIDOperation(
              didData = Some(
                CreateDIDOperation.DIDCreationData(
                  publicKeys = List(
                    PublicKey(
                      id = PrismDid.DEFAULT_MASTER_KEY_ID,
                      usage = KeyUsage.MASTER_KEY,
                      keyData = PublicKey.KeyData.CompressedEcKeyData(masterKey.toProto)
                    )
                  )
                )
              )
            )
          )
        )

      expectedInitialState mustBe long.initialState
    }

    "correctly generate suffix" in {
      val hash = Sha256Hash.compute(Array[Byte](0))
      val input = PrismDid.buildCanonical(Sha256Hash.compute(Array[Byte](0)))

      hash.hexEncoded mustBe input.suffix
    }

    "correctly build a valid did from empty canonical string" in {
      val validDid = PrismDid.buildCanonical(Sha256Hash.compute(Array[Byte](0)))
      val unsafeDid = PrismDid.fromString(validDid.value)

      validDid mustBe unsafeDid
    }

    "correctly build from valid long form DID" in {
      val longAsString = long.value
      val unsafeDid = PrismDid.fromString(longAsString)

      long mustBe unsafeDid
    }

    "correctly build from valid short form did" in {
      val canonicalAsString = short.value
      val unsafeDid = PrismDid.fromString(canonicalAsString)

      short mustBe unsafeDid
    }

    "parse prism did into a long form did and extract keys from DID data" in {
      val didString =
        "did:prism:1e8777cf1e014563b123d6eed984ff35d235f64497e6736b7b9647649b6afe8f:CmIKYBJeCgdtYXN0ZXIwEAFCUQoJc2VjcDI1NmsxEiEAwCb_BYvKwhcOIAWiguHbdBfRgJWVO9EvBgWGHPKn9wYaIQDYr0B_6ZsLlfhdE9Nv8-_sZP-l-u8UeUCSbucNiDrrrg"
      val prismDid = PrismDid.fromString(didString)
      assert(prismDid.isInstanceOf[LongFormPrismDid])

      val prismState = prismDid.asInstanceOf[LongFormPrismDid].initialState
      val didData = prismState.operation.createDid.get.didData.value
      val publicKey = didData.publicKeys.head

      val xProtoBytes = publicKey.getEcKeyData.x.toByteArray
      val yProtoBytes = publicKey.getEcKeyData.y.toByteArray
      xProtoBytes.length mustBe 33
      yProtoBytes.length mustBe 33
    }

    "catch failure when parsing invalid DID" in {
      val caught = intercept[IllegalArgumentException] {
        PrismDid.fromString("invalid-did")
      }

      assert(caught.getMessage == "Invalid DID format: invalid-did")
    }

    "create canonical from string correctly" in {
      val shortAsString = short.value
      val unsafeDid = PrismDid.canonicalFromString(shortAsString)

      short mustBe unsafeDid
    }

    "fail when long form initial state is not CreateDid" in {
      val mockAtalaOperation =
        AtalaOperation(operation = AtalaOperation.Operation.UpdateDid(UpdateDIDOperation()))
      val updateDid = createLongFormDidFromAtalaOperationUnsafely(mockAtalaOperation)

      val caught = intercept[CreateDidExpectedAsInitialState] {
        PrismDid.fromString(updateDid.did.toString())
      }

      caught.getMessage mustBe "Provided initial state of long form Prism DID is UpdateDIDOperation(<ByteString@18e36d14 size=0 contents=\"\">,,Vector(),UnknownFieldSet(Map())), CreateDid Atala operation expected"

    }

    "fail for long form where master key is not present" in {
      val issuingPublicKey = CryptoTestUtils.generateKeyPair().publicKey
      val revocationPublicKey = CryptoTestUtils.generateKeyPair().publicKey

      val issuingKeyPublicKey =
        PublicKey(
          id = PrismDid.DEFAULT_ISSUING_KEY_ID,
          usage = KeyUsage.ISSUING_KEY,
          keyData = PublicKey.KeyData.CompressedEcKeyData(issuingPublicKey.toProto)
        )
      val revocationKeyPublicKey =
        PublicKey(
          id = PrismDid.DEFAULT_REVOCATION_KEY_ID,
          usage = KeyUsage.REVOCATION_KEY,
          keyData = PublicKey.KeyData.CompressedEcKeyData(revocationPublicKey.toProto)
        )

      val createDidOp = CreateDIDOperation(
        didData = Some(
          CreateDIDOperation.DIDCreationData(
            publicKeys = List(issuingKeyPublicKey, revocationKeyPublicKey)
          )
        )
      )

      val noMasterKeyCreateDid =
        AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))
      val longForm = createLongFormDidFromAtalaOperationUnsafely(noMasterKeyCreateDid)

      val caught = intercept[IllegalArgumentException] {
        PrismDid.fromString(longForm.did.toString())
      }

      caught.getMessage mustBe "requirement failed: At least one public key with master role required"

    }

  }
}
