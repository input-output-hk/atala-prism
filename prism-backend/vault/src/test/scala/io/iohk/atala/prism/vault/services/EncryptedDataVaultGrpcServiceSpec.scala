package io.iohk.atala.prism.vault.services

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Base64, UUID}
import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.protos.{common_models, vault_api}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation.UpdateDid
import io.iohk.atala.prism.protos.node_models.UpdateDIDOperation
import io.iohk.atala.prism.protos.vault_api.StoreDataRequest
import io.iohk.atala.prism.utils.Base64Utils.decodeURL
import io.iohk.atala.prism.vault.VaultRpcSpecBase
import org.scalatest.OptionValues

import scala.util.Try

class EncryptedDataVaultGrpcServiceSpec extends VaultRpcSpecBase with OptionValues {
  private def createRequest(
      externalId: UUID,
      payload: String
  ): StoreDataRequest = {
    val payloadBytes = payload.getBytes()
    val hash = Sha256.compute(payloadBytes).getValue.toArray
    vault_api.StoreDataRequest(
      externalId = externalId.toString,
      payloadHash = ByteString.copyFrom(hash),
      payload = ByteString.copyFrom(payloadBytes)
    )
  }

  private def createRequest(payload: String): StoreDataRequest = {
    val externalId = UUID.randomUUID()
    createRequest(externalId, payload)
  }

  "health check" should {
    "respond" in {
      val response = vaultGrpcService
        .healthCheck(common_models.HealthCheckRequest())
        .futureValue
      response must be(common_models.HealthCheckResponse())
    }
  }

  "store" should {
    "create a payload" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val payload = "encrypted_data"
      val request = createRequest(payload)
      val rpcRequest = SignedRpcRequest.generate(keys, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val responsePayloadId = serviceStub.storeData(request).payloadId

        val storedPayloads =
          payloadsRepository
            .getByPaginated(did, None, 10)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()

        storedPayloads.size must be(1)
        val storedPayload = storedPayloads.head

        storedPayload.id.toString must be(responsePayloadId)
        storedPayload.did must be(did.asCanonical())
        storedPayload.content must be(payload.toVector)
        assert(
          storedPayload.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2
        )
      }
    }

    "be idempotent on the exact same request" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val payload = "encrypted_data"
      val request = createRequest(payload)
      val rpcRequest1 = SignedRpcRequest.generate(keys, did, request)
      val rpcRequest2 = SignedRpcRequest.generate(keys, did, request)

      val id1 = usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.storeData(request).payloadId
      }

      val id2 = usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeData(request).payloadId
      }

      id1 must be(id2)

      val storedPayloads =
        payloadsRepository
          .getByPaginated(did, None, 10)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()

      // There must only be one payload stored
      storedPayloads.size must be(1)
      val storedPayload = storedPayloads.head

      storedPayload.id.toString must be(id1)
      storedPayload.did must be(did.asCanonical())
      storedPayload.content must be(payload.toVector)
      assert(
        storedPayload.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2
      )
    }

    "fail on the multiple different requests with the same id" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val externalId = UUID.randomUUID()
      val request1 = createRequest(externalId, "encrypted_data_1")
      val rpcRequest1 = SignedRpcRequest.generate(keys, did, request1)
      val request2 = createRequest(externalId, "encrypted_data_2")
      val rpcRequest2 = SignedRpcRequest.generate(keys, did, request2)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.storeData(request1)
      }

      usingApiAs(rpcRequest2) { serviceStub =>
        intercept[RuntimeException] {
          serviceStub.storeData(request2)
        }
      }
    }
  }

  "get" should {
    "return all created payloads" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val payload1 = "encrypted_data_1"
      val request1 = createRequest(payload1)
      val rpcRequest1 = SignedRpcRequest.generate(keys, did, request1)

      val id1 = usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.storeData(request1).payloadId
      }

      val payload2 = "encrypted_data_1"
      val request2 = createRequest(payload2)
      val rpcRequest2 = SignedRpcRequest.generate(keys, did, request2)
      val id2 = usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeData(request2).payloadId
      }

      val request3 = vault_api.GetPaginatedDataRequest(limit = 5)
      val rpcRequest3 = SignedRpcRequest.generate(keys, did, request3)
      usingApiAs(rpcRequest3) { serviceStub =>
        val payloads1 = serviceStub
          .getPaginatedData(request3)
          .payloads

        payloads1.size must be(2)
        assert(payloads1.exists(_.id == id1))
        assert(payloads1.exists(_.id == id2))
      }

      val request4 = vault_api.GetPaginatedDataRequest(limit = 1)
      val rpcRequest4 = SignedRpcRequest.generate(keys, did, request4)
      val payloads2 = usingApiAs(rpcRequest4) { serviceStub =>
        val payloads2 = serviceStub
          .getPaginatedData(request4)
          .payloads

        payloads2.size must be(1)
        assert(payloads2.exists(_.id == id1))
        payloads2
      }

      val request5 = vault_api.GetPaginatedDataRequest(
        lastSeenId = payloads2.head.id,
        limit = 1
      )
      val rpcRequest5 = SignedRpcRequest.generate(keys, did, request5)
      usingApiAs(rpcRequest5) { serviceStub =>
        val payloads3 = serviceStub
          .getPaginatedData(request5)
          .payloads

        payloads3.size must be(1)
        assert(payloads3.exists(_.id == id2))
      }
    }
  }

  "authentication" should {
    "support unpublished DID authentication" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest = SignedRpcRequest.generate(keys, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getPaginatedData(request)
        response must be(vault_api.GetPaginatedDataResponse())
      }
    }

    "reject DIDs with non-matching hash" in {
      val keys1 = EC.generateKeyPair()
      val keys2 = EC.generateKeyPair()
      val did1 = DID.buildLongFormFromMasterPublicKey(keys1.getPublicKey)
      val did2 = DID.buildLongFormFromMasterPublicKey(keys2.getPublicKey)
      val fakeDid = Try(
        DID.buildLongForm(
          Sha256Digest.fromHex(did1.asCanonical().getSuffix),
          decodeURL(did2.getSuffix.dropWhile(_ != ':').tail)
        )
      )
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest =
        fakeDid.map(fake => SignedRpcRequest.generate(keys1, fake, request))
      rpcRequest.map { req =>
        usingApiAs(req) { blockingStub =>
          intercept[RuntimeException] {
            blockingStub.getPaginatedData(request)
          }
        }
      }.isFailure must be(true)
    }

    "reject DIDs with malformed encoded operation inside" in {
      val keys = EC.generateKeyPair()
      val operationBytes = Array(100.toByte, 200.toByte)
      val operationHash = Sha256.compute(operationBytes)
      val didCanonicalSuffix = operationHash.getHexValue
      val encodedOperation =
        Base64.getUrlEncoder.withoutPadding().encode(operationBytes)
      val did = Try(
        DID.buildLongForm(
          Sha256Digest.fromHex(didCanonicalSuffix),
          encodedOperation
        )
      )
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest =
        did.map(lDid => SignedRpcRequest.generate(keys, lDid, request))
      rpcRequest.map { req =>
        usingApiAs(req) { blockingStub =>
          intercept[RuntimeException] {
            blockingStub.getPaginatedData(request)
          }
        }
      }.isFailure must be(true)
    }

    "reject DIDs with no CreateDID operation inside" in {
      val keys = EC.generateKeyPair()
      val operation =
        node_models.AtalaOperation(UpdateDid(UpdateDIDOperation()))
      val operationBytes = operation.toByteArray
      val operationHash = Sha256.compute(operationBytes)
      val didCanonicalSuffix = operationHash.getHexValue
      val encodedOperation =
        Base64.getUrlEncoder.withoutPadding().encode(operationBytes)
      val did = Try(
        DID.buildLongForm(
          Sha256Digest.fromHex(didCanonicalSuffix),
          encodedOperation
        )
      )
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest =
        did.map(lDid => SignedRpcRequest.generate(keys, lDid, request))
      rpcRequest.map { req =>
        usingApiAs(req) { blockingStub =>
          intercept[RuntimeException] {
            blockingStub.getPaginatedData(request)
          }
        }
      }.isFailure must be(true)
    }

    "reject DIDs with invalid key id" in {
      val keys = EC.generateKeyPair()
      val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest =
        SignedRpcRequest.generate(keys, did, request).copy(keyId = "missing0")
      usingApiAs(rpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.getPaginatedData(request)
        }
      }
    }

    "reject invalid signatures" in {
      val keys1 = EC.generateKeyPair()
      val did1 = DID.buildLongFormFromMasterPublicKey(keys1.getPublicKey)
      val request = vault_api.GetPaginatedDataRequest()
      val rpcRequest1 = SignedRpcRequest.generate(keys1, did1, request)
      val malformedSignature = Array(100.toByte, 200.toByte)
      val malformedSignatureRpcRequest = rpcRequest1
        .copy(signature = new ECSignature(malformedSignature))
      usingApiAs(malformedSignatureRpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.getPaginatedData(request)
        }
      }

      val keys2 = EC.generateKeyPair()
      val did2 = DID.buildLongFormFromMasterPublicKey(keys2.getPublicKey)
      val rpcRequest2 = SignedRpcRequest.generate(keys2, did2, request)
      val invalidSignatureRpcRequest =
        rpcRequest1.copy(signature = rpcRequest2.signature)
      usingApiAs(invalidSignatureRpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.getPaginatedData(request)
        }
      }
    }
  }
}
