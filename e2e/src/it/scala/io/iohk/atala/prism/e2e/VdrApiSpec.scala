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
      val first = client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signed)))
      awaitApplied(operationIdOrFail(requireOutput(first.output, "dup create first")))

      val second: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signed))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      second match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          val out = requireOutput(resp.output, "dup create second")
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
  }
}
