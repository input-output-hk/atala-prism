package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}

class VdrApiSpec extends VdrTestUtils {

  "VDR API edges" should {
    "reject duplicate VDR create (same payload/nonce)" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val op = node_models.AtalaOperation().withCreateStorageEntry(
        node_models.CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("dup")))
      )
      val signed = signOperation(op, "vdr", vdr.privateKey)
      val first = client.scheduleOperations(
        node_api.ScheduleOperationsRequest(signedOperations = Seq(signed))
      )
      awaitApplied(operationIdOrFail(requireOutput(first.outputs.headOption.getOrElse(fail("no output")), "dup create first")))

      val second: Either[StatusRuntimeException, node_api.ScheduleOperationsResponse] =
        try {
          Right(client.scheduleOperations(node_api.ScheduleOperationsRequest(signedOperations = Seq(signed))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      second match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.outputs.headOption.getOrElse(fail("no output")), "dup create second")
          val opId = operationIdOrFail(out)
          val status = awaitFinal(opId)
          status should (be(common_models.OperationStatus.CONFIRMED_AND_APPLIED)
            .or(be(common_models.OperationStatus.CONFIRMED_AND_REJECTED)))
      }
    }

    "verify returns false for non-existent event hash" taggedAs E2ETestTag in {
      val missing = ByteString.copyFrom(Sha256Hash.compute("missing".getBytes()).bytes.toArray)
      val verifyResp = client.verifyVdrEntry(node_api.VerifyVdrEntryRequest(eventHash = missing))
      verifyResp.valid shouldBe false
    }

    "getVdrEntry returns NOT_FOUND for unknown hash" taggedAs E2ETestTag in {
      val missing = ByteString.copyFrom(Sha256Hash.compute("missing-get".getBytes()).bytes.toArray)
      val ex = intercept[StatusRuntimeException] {
        client.getVdrEntry(node_api.GetVdrEntryRequest(missing))
      }
      ex.getStatus.getCode should (be(io.grpc.Status.NOT_FOUND.getCode).or(be(io.grpc.Status.UNKNOWN.getCode)))
    }

    "verify and get work for deactivated entries and historical hashes" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didHash = createDidWithVdrKey(master, vdr)

      val (createHash, _) = createVdrEntry(didHash, vdr, "hist-1")
      val updateHash = updateVdrEntry(createHash, vdr, "hist-2")

      val deactivateOp = node_models.AtalaOperation().withDeactivateStorageEntry(
        node_models.DeactivateStorageEntryOperation().withPreviousEventHash(updateHash)
      )
      val signedDeactivate = signOperation(deactivateOp, "vdr", vdr.privateKey)
      val deactivateResp = client.scheduleOperations(
        node_api.ScheduleOperationsRequest(signedOperations = Seq(signedDeactivate))
      )
      val deactivateOut = requireOutput(deactivateResp.outputs.headOption.getOrElse(fail("missing deactivate out")), "deactivate op")
      val deactivateHash = deactivateOut.result.deactivateVdrEntryOutput
        .map(_.eventHash).getOrElse(fail("missing deactivate hash"))
      awaitApplied(operationIdOrFail(deactivateOut))

      val verifyCreate = client.verifyVdrEntry(node_api.VerifyVdrEntryRequest(createHash))
      val verifyUpdate = client.verifyVdrEntry(node_api.VerifyVdrEntryRequest(updateHash))
      val verifyDeactivate = client.verifyVdrEntry(node_api.VerifyVdrEntryRequest(deactivateHash))

      verifyCreate.valid shouldBe true
      verifyUpdate.valid shouldBe true
      verifyDeactivate.valid shouldBe true

      val gotDeactivate = client.getVdrEntry(node_api.GetVdrEntryRequest(createHash)).entry.getOrElse(
        fail("missing deactivate entry via root hash")
      )
      gotDeactivate.eventHash shouldBe deactivateHash
      gotDeactivate.status shouldBe node_api.VdrEntryStatus.DEACTIVATED
    }
  }
}
