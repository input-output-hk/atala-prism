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
        client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signedCreateStorage)))

      val createVdrOutput = requireOutput(createVdrResp.output, "create VDR")
      val createVdrOpId = operationIdOrFail(createVdrOutput)
      val createEventHash = require(createVdrOutput.result.createVdrEntryOutput, "create VDR event hash").eventHash
      awaitApplied(createVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val createdEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(createEventHash)).entry,
        "created entry"
      )
      createdEntry.deactivated shouldBe false
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
        client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdateStorage)))

      val updateOutput = requireOutput(updateResp.output, "update VDR")
      val updateVdrOpId = operationIdOrFail(updateOutput)
      val updateEventHash = require(updateOutput.result.updateVdrEntryOutput, "update VDR event hash").eventHash
      awaitApplied(updateVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val updatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(updateEventHash)).entry,
        "updated entry"
      )
      updatedEntry.deactivated shouldBe false
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
        client.deactivateVdrEntry(node_api.DeactivateVdrEntryRequest(Some(signedDeactivate)))

      val deactivateOutput = requireOutput(deactivateResp.output, "deactivate VDR")
      val deactivateOpId = operationIdOrFail(deactivateOutput)
      val deactivateEventHash =
        require(deactivateOutput.result.deactivateVdrEntryOutput, "deactivate VDR event hash").eventHash
      awaitApplied(deactivateOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val deactivatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(deactivateEventHash)).entry,
        "deactivated entry"
      )
      deactivatedEntry.deactivated shouldBe true
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

      val created = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(ByteString.copyFrom(createDigest.bytes.toArray))).entry,
        "created entry via schedule"
      )
      created.data.flatMap(_.content.bytes.map(_.toStringUtf8)) shouldBe Some("via-schedule-1")

      val updated = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(ByteString.copyFrom(updateDigest.bytes.toArray))).entry,
        "updated entry via schedule"
      )
      updated.data.flatMap(_.content.ipfs) shouldBe Some("cid-via-schedule")
      val deactivated = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(ByteString.copyFrom(Sha256Hash.compute(deactivateOp.toByteArray).bytes.toArray))).entry,
        "deactivated entry via schedule"
      )
      deactivated.deactivated shouldBe true
    }
  }
}
