package io.iohk.atala.prism.vault.services

import java.util.Base64

import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.{EC, ECSignature, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation.UpdateDid
import io.iohk.atala.prism.protos.node_models.UpdateDIDOperation
import io.iohk.atala.prism.vault.VaultRpcSpecBase

class EncryptedDataVaultServiceImplSpec extends VaultRpcSpecBase {
  "health check" should {
    "respond" in {
      val response = vaultService.healthCheck(vault_api.HealthCheckRequest()).futureValue
      response must be(vault_api.HealthCheckResponse())
    }
  }

  "authentication" should {
    "support unpublished DID authentication" in {
      val keys = EC.generateKeyPair()
      val did = DID.createUnpublishedDID(keys.publicKey)
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest = SignedRpcRequest.generate(keys, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.authHealthCheck(request)
        response must be(vault_api.AuthHealthCheckResponse())
      }
    }

    "reject DIDs with non-matching hash" in {
      val keys1 = EC.generateKeyPair()
      val keys2 = EC.generateKeyPair()
      val did1 = DID.createUnpublishedDID(keys1.publicKey)
      val did2 = DID.createUnpublishedDID(keys2.publicKey)
      val fakeDid = DID.buildPrismDID(
        DID.getCanonicalSuffix(did1).get,
        DID.stripPrismPrefix(did2).dropWhile(_ != ':').tail
      )
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest = SignedRpcRequest.generate(keys1, fakeDid, request)
      usingApiAs(rpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }
    }

    "reject DIDs with malformed encoded operation inside" in {
      val keys = EC.generateKeyPair()
      val operationBytes = Array(100.toByte, 200.toByte)
      val operationHash = SHA256Digest.compute(operationBytes)
      val didCanonicalSuffix = operationHash.hexValue
      val encodedOperation = Base64.getUrlEncoder.withoutPadding().encodeToString(operationBytes)
      val did = DID.buildPrismDID(didCanonicalSuffix, encodedOperation)
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest = SignedRpcRequest.generate(keys, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }
    }

    "reject DIDs with no CreateDID operation inside" in {
      val keys = EC.generateKeyPair()
      val operation = node_models.AtalaOperation(UpdateDid(UpdateDIDOperation()))
      val operationBytes = operation.toByteArray
      val operationHash = SHA256Digest.compute(operationBytes)
      val didCanonicalSuffix = operationHash.hexValue
      val encodedOperation = Base64.getUrlEncoder.withoutPadding().encodeToString(operationBytes)
      val did = DID.buildPrismDID(didCanonicalSuffix, encodedOperation)
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest = SignedRpcRequest.generate(keys, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }
    }

    "reject DIDs with invalid key id" in {
      val keys = EC.generateKeyPair()
      val did = DID.createUnpublishedDID(keys.publicKey)
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest = SignedRpcRequest.generate(keys, did, request).copy(keyId = "missing0")
      usingApiAs(rpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }
    }

    "reject invalid signatures" in {
      val keys1 = EC.generateKeyPair()
      val did1 = DID.createUnpublishedDID(keys1.publicKey)
      val request = vault_api.AuthHealthCheckRequest()
      val rpcRequest1 = SignedRpcRequest.generate(keys1, did1, request)
      val malformedSignature = Array(100.toByte, 200.toByte)
      val malformedSignatureRpcRequest = rpcRequest1
        .copy(signature = ECSignature(malformedSignature))
      usingApiAs(malformedSignatureRpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }

      val keys2 = EC.generateKeyPair()
      val did2 = DID.createUnpublishedDID(keys2.publicKey)
      val rpcRequest2 = SignedRpcRequest.generate(keys2, did2, request)
      val invalidSignatureRpcRequest = rpcRequest1.copy(signature = rpcRequest2.signature)
      usingApiAs(invalidSignatureRpcRequest) { blockingStub =>
        intercept[RuntimeException] {
          blockingStub.authHealthCheck(request)
        }
      }
    }
  }
}
