package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpECDSA
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import scala.concurrent.duration._

class VdrNegativeSpec extends VdrTestUtils {

  private def expectInvalidArgOr[A](block: => A): Option[A] =
    try Some(block)
    catch {
      case ex: StatusRuntimeException if ex.getStatus.getCode == io.grpc.Status.INVALID_ARGUMENT.getCode => None
    }

  "VDR negative cases" should {
    "reject VDR create when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val badSignedCreate = signOperation(
        node_models.AtalaOperation().withCreateStorageEntry(
          node_models.CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
        ),
        keyId = "master", // wrong usage
        key = master.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(badSignedCreate)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "create VDR with bad key"),
          "create VDR with bad key"
        )
      }
    }

    "reject VDR update with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("deadbeef")
      val signedUpdate = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(bogusPrev)
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("x")))
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedUpdate)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "update unknown prev hash"),
          "update unknown prev hash"
        )
      }
    }

    "reject VDR deactivate with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("cafebabe")
      val signedDeactivate = signOperation(
        node_models.AtalaOperation().withDeactivateStorageEntry(
          node_models.DeactivateStorageEntryOperation().withPreviousEventHash(bogusPrev)
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedDeactivate)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "deactivate unknown prev hash"),
          "deactivate unknown prev hash"
        )
      }
    }

    "reject VDR create when DID has no VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val didSuffixHash = createDidWithoutVdr(master)

      val signedCreate = signOperation(
        node_models.AtalaOperation().withCreateStorageEntry(
          node_models.CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
        ),
        keyId = "master",
        key = master.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreate)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "create VDR without VDR key"),
          "create VDR without VDR key"
        )
      }
    }

    "reject VDR create when VDR key curve is not secp256k1" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val ex = intercept[StatusRuntimeException] {
        createDidWithCustomVdr(master, vdr.publicKey, curveOverride = "ed25519")
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }

    "reject VDR create with invalid signature" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val createOp = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
      )
      val badSigned = node_models.SignedAtalaOperation(
        signedWith = "vdr",
        signature = ByteString.EMPTY, // invalid signature
        operation = Some(createOp)
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(badSigned)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "create VDR with invalid signature"),
          "create VDR with invalid signature"
        )
      }
    }

    "reject VDR update when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)
      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")

      val signedUpdateWithMaster = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(createEventHash)
            .withData(node_models.StorageData().withIpfs("cid-2"))
        ),
        keyId = "master",
        key = master.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedUpdateWithMaster)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "update VDR with master key"),
          "update VDR with master key"
        )
      }
    }

    "reject VDR deactivate when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)
      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")
      val updateEventHash = updateVdrEntry(createEventHash, vdr, "cid-2")

      val signedDeactivateWithMaster = signOperation(
        node_models.AtalaOperation().withDeactivateStorageEntry(
          node_models.DeactivateStorageEntryOperation()
            .withPreviousEventHash(updateEventHash)
        ),
        keyId = "master",
        key = master.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedDeactivateWithMaster)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "deactivate VDR with master key"),
          "deactivate VDR with master key"
        )
      }
    }

    "reject VDR create when using VDR key from another DID" taggedAs E2ETestTag in {
      val masterA = generateKeyPair()
      val vdrA = generateKeyPair()
      val masterB = generateKeyPair()
      val vdrB = generateKeyPair()

      val didA = createDidWithVdrKey(masterA, vdrA)
      createDidWithVdrKey(masterB, vdrB)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didA.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("cross")))
      )
      val signed = signOperation(op, "vdr", vdrB.privateKey)

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signed)))
        val out = requireOutput(resp.outputs.headOption, "cross-DID VDR key")
        expectRejectedOutput(out, "cross-DID VDR key")
      }
    }

    "reject VDR create after VDR key was removed from DID" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      removeVdrKeyFromDid(didHash, master)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("after-removal")))
      )
      val signed = signOperation(op, "vdr", vdr.privateKey)

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signed)))
        expectRejectedOutput(
          requireOutput(resp.outputs.headOption, "create after VDR removal"),
          "create after VDR removal"
        )
      }
    }

    "reject VDR update after entry was deactivated" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      val (createEvent, _) = createVdrEntry(didHash, vdr, "payload-1")
      val updateEvent = updateVdrEntry(createEvent, vdr, "cid-2")

      val deactivateOp = node_models.AtalaOperation().withDeactivateStorageEntry(
        node_models.DeactivateStorageEntryOperation().withPreviousEventHash(updateEvent)
      )
      val signedDeactivate = signOperation(deactivateOp, "vdr", vdr.privateKey)
      val deactivateResp =
        client.scheduleOperations(node_api.ScheduleOperationsRequest(signedOperations = Seq(signedDeactivate)))
      awaitApplied(operationIdOrFail(requireOutput(deactivateResp.outputs.headOption, "deactivate before update")))

      val signedUpdateAfterDeactivate = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(updateEvent)
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("should-fail")))
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedUpdateAfterDeactivate)))
        expectRejectedOutput(requireOutput(resp.outputs.headOption, "update after deactivate"), "update after deactivate")
      }
    }

    "reject VDR create when signedWith references unknown key id" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("unknown-signer")))
      )

      val signed = node_models.SignedAtalaOperation(
        signedWith = "unknown-key-id",
        operation = Some(op),
        signature = ByteString.copyFrom(SecpECDSA.signBytes(op.toByteArray, vdr.privateKey).bytes)
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signed)))
        expectRejectedOutput(requireOutput(resp.outputs.headOption, "unknown key id"), "unknown key id")
      }
    }

    "reject VDR create with malformed signature bytes" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("bad-sig")))
      )

      val badSig = ByteString.copyFrom(Array.fill[Byte](8)(0x01.toByte)) // clearly invalid ECDSA length
      val signed = node_models.SignedAtalaOperation(
        signedWith = "vdr",
        operation = Some(op),
        signature = badSig
      )

      expectInvalidArgOr {
        val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signed)))
        expectRejectedOutput(requireOutput(resp.outputs.headOption, "malformed signature"), "malformed signature")
      }
    }

    "surface errors when scheduling mixed valid/invalid VDR operations" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      val createOp = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("mixed-ok")))
      )
      val signedCreate = signOperation(createOp, "vdr", vdr.privateKey)

      val badUpdateOp = node_models.AtalaOperation().withUpdateStorageEntry(
        node_models.UpdateStorageEntryOperation()
          .withPreviousEventHash(ByteString.copyFromUtf8("bogus"))
          .withData(node_models.StorageData().withIpfs("should-fail"))
      )
      val signedBadUpdate = signOperation(badUpdateOp, "vdr", vdr.privateKey)

      val respOrEx = try {
        Right(client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreate, signedBadUpdate))))
      } catch {
        case ex: StatusRuntimeException => Left(ex)
      }

      respOrEx match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          resp.outputs.size shouldBe 2

          val createOut = resp.outputs.head
          val updateOut = resp.outputs(1)

          val createId = operationIdOrFail(createOut)

          awaitFinal(createId, 240.seconds) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

          // The invalid update should surface an error eagerly or produce a rejected operation id.
          updateOut.operationMaybe.error.orElse(updateOut.operationMaybe.operationId) should not be empty
          updateOut.operationMaybe.error.foreach { err =>
            err should not be empty
          }
          updateOut.operationMaybe.operationId.foreach { id =>
            awaitRejectedOrPending(id, 240.seconds) should (be(common_models.OperationStatus.CONFIRMED_AND_REJECTED)
              .or(be(common_models.OperationStatus.PENDING_SUBMISSION)))
          }
      }
    }
  }
}
