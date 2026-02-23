package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}

class VdrHappyPathSpec extends VdrTestUtils {

  "VDR happy path" should {
    "create a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val createStorageOp = node_models
        .AtalaOperation()
        .withCreateStorageEntry(
          node_models
            .CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload-1")))
        )
      val signedCreateStorage = signOperation(createStorageOp, "vdr", vdr.privateKey)
      val createVdrResp =
        client.scheduleOperations(node_api.ScheduleOperationsRequest(signedOperations = Seq(signedCreateStorage)))

      val createVdrOutput = requireOutput(createVdrResp.outputs.headOption, "create VDR")
      val createVdrOpId = operationIdOrFail(createVdrOutput)
      val createEventHash = require(createVdrOutput.result.createVdrEntryOutput, "create VDR event hash").eventHash
      awaitApplied(createVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val createdEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(createEventHash)).entry,
        "created entry"
      )
      createdEntry.status shouldBe node_api.VdrEntryStatus.ACTIVE
      createdEntry.data.flatMap(_.content.bytes) shouldBe Some(ByteString.copyFromUtf8("payload-1"))
      createdEntry.nonce shouldBe ByteString.EMPTY
    }

    "update a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")

      val updateStorageOp = node_models
        .AtalaOperation()
        .withUpdateStorageEntry(
          node_models
            .UpdateStorageEntryOperation()
            .withPreviousEventHash(createEventHash)
            .withData(node_models.StorageData().withIpfs("cid-2"))
        )
      val signedUpdateStorage = signOperation(updateStorageOp, "vdr", vdr.privateKey)
      val updateResp =
        client.scheduleOperations(node_api.ScheduleOperationsRequest(signedOperations = Seq(signedUpdateStorage)))

      val updateOutput = requireOutput(updateResp.outputs.headOption, "update VDR")
      val updateVdrOpId = operationIdOrFail(updateOutput)
      val updateEventHash = require(updateOutput.result.updateVdrEntryOutput, "update VDR event hash").eventHash
      awaitApplied(updateVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val updatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(createEventHash)).entry,
        "updated entry (by root hash)"
      )
      updatedEntry.eventHash shouldBe updateEventHash
      updatedEntry.status shouldBe node_api.VdrEntryStatus.ACTIVE
      updatedEntry.data.flatMap(_.content.ipfs) shouldBe Some("cid-2")
      updatedEntry.previousEventHash shouldBe createEventHash
    }

    "deactivate a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")
      val updateEventHash = updateVdrEntry(createEventHash, vdr, "cid-2")

      val deactivateStorageOp = node_models
        .AtalaOperation()
        .withDeactivateStorageEntry(
          node_models
            .DeactivateStorageEntryOperation()
            .withPreviousEventHash(updateEventHash)
        )
      val signedDeactivate = signOperation(deactivateStorageOp, "vdr", vdr.privateKey)
      val deactivateResp =
        client.scheduleOperations(node_api.ScheduleOperationsRequest(signedOperations = Seq(signedDeactivate)))

      val deactivateOutput = requireOutput(deactivateResp.outputs.headOption, "deactivate VDR")
      val deactivateOpId = operationIdOrFail(deactivateOutput)
      val deactivateEventHash =
        require(deactivateOutput.result.deactivateVdrEntryOutput, "deactivate VDR event hash").eventHash
      awaitApplied(deactivateOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val deactivatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(createEventHash)).entry,
        "deactivated entry (by root hash)"
      )
      deactivatedEntry.eventHash shouldBe deactivateEventHash
      deactivatedEntry.status shouldBe node_api.VdrEntryStatus.DEACTIVATED
      deactivatedEntry.previousEventHash shouldBe updateEventHash
    }

    "verify VDR entry returns valid=true for applied entry" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)
      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-verify")
      val verifyResp = client.verifyVdrEntry(node_api.VerifyVdrEntryRequest(eventHash = createEventHash))
      verifyResp.valid shouldBe true
    }

    "apply VDR operations via scheduleOperations" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      val createOp = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("via-schedule-1")))
      )
      val createDigest = Sha256Hash.compute(createOp.toByteArray)
      val signedCreate = signOperation(createOp, "vdr", vdr.privateKey)

      val updateOp = node_models.AtalaOperation().withUpdateStorageEntry(
        node_models.UpdateStorageEntryOperation()
          .withPreviousEventHash(ByteString.copyFrom(createDigest.bytes.toArray))
          .withData(node_models.StorageData().withIpfs("cid-via-schedule"))
      )
      val updateDigest = Sha256Hash.compute(updateOp.toByteArray)
      val signedUpdate = signOperation(updateOp, "vdr", vdr.privateKey)

      val deactivateOp = node_models.AtalaOperation().withDeactivateStorageEntry(
        node_models.DeactivateStorageEntryOperation()
          .withPreviousEventHash(ByteString.copyFrom(updateDigest.bytes.toArray))
      )
      val signedDeactivate = signOperation(deactivateOp, "vdr", vdr.privateKey)

      val resp = client.scheduleOperations(
        node_api.ScheduleOperationsRequest(
          signedOperations = Seq(signedCreate, signedUpdate, signedDeactivate)
        )
      )
      resp.outputs.size shouldBe 3
      val ids = resp.outputs.map(operationIdOrFail)
      ids.foreach(id => awaitApplied(id))

      val headAfterAll = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(ByteString.copyFrom(createDigest.bytes.toArray))).entry,
        "head entry via schedule (root hash)"
      )
      headAfterAll.eventHash shouldBe ByteString.copyFrom(Sha256Hash.compute(deactivateOp.toByteArray).bytes.toArray)
      headAfterAll.status shouldBe node_api.VdrEntryStatus.DEACTIVATED
      headAfterAll.previousEventHash shouldBe ByteString.copyFrom(updateDigest.bytes.toArray)
    }
  }
}
