package io.iohk.atala.prism.management.console.services

import com.google.protobuf.ByteString
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.management.console.DataPreparation.{createParticipant, getBatchData}
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.models.Ledger.InMemory
import io.iohk.atala.prism.protos.{common_models, console_api, node_api, node_models}
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.Mockito.verify
import org.scalatest.OptionValues.convertOptionToValuable

import scala.concurrent.Future

class CredentialsServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {

  "CredentialsServiceImpl.getLedgerData" should {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.publicKey)
    val aHash = SHA256Digest.compute("random".getBytes())
    val aCredentialHash = ByteString.copyFrom(aHash.value.toArray)
    val illegalCredentialHash = ByteString.copyFrom(aHash.value.drop(1).toArray)
    val aBatchId = CredentialBatchId.random().id

    "fail when queried with invalid batchId" in {
      val illegalBatchId = "@@@#!?"
      createParticipant("Institution", did)
      val request = console_api.GetLedgerDataRequest(illegalBatchId, aCredentialHash)
      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.getLedgerData(request)
        )
      }
    }

    "fail when queried with invalid credentialHash" in {
      createParticipant("Institution", did)
      val request = console_api.GetLedgerDataRequest(aBatchId, illegalCredentialHash)
      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.getLedgerData(request)
        )
      }
    }

    val batchIssuanceLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 1",
        ledger = common_models.Ledger.IN_MEMORY,
        timestampInfo = Some(node_models.TimestampInfo(1, 1, 1))
      )

    val batchRevocationLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 2",
        ledger = common_models.Ledger.CARDANO_MAINNET,
        timestampInfo = Some(node_models.TimestampInfo(2, 2, 2))
      )

    val credentialRevocationLedgerData =
      node_models.LedgerData(
        transactionId = "tx id 3",
        ledger = common_models.Ledger.CARDANO_TESTNET,
        timestampInfo = Some(node_models.TimestampInfo(3, 3, 3))
      )

    def nodeReturns(
        batchIssuanceLedgerData: Option[node_models.LedgerData],
        batchRevocationLedgerData: Option[node_models.LedgerData],
        credentialRevocationLedgerData: Option[node_models.LedgerData]
    ): Unit = {
      val aMerkleRoot = ByteString.copyFrom(SHA256Digest.compute("root".getBytes()).value.toArray)
      nodeMock
        .getBatchState(node_api.GetBatchStateRequest(aBatchId))
        .returns(
          Future.successful(
            node_api
              .GetBatchStateResponse("did:prism:aDID", aMerkleRoot, batchIssuanceLedgerData, batchRevocationLedgerData)
          )
        )
      nodeMock
        .getCredentialRevocationTime(node_api.GetCredentialRevocationTimeRequest(aBatchId, aCredentialHash))
        .returns(
          Future.successful(node_api.GetCredentialRevocationTimeResponse(credentialRevocationLedgerData))
        )
      ()
    }

    "return batch issuance data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(Some(batchIssuanceLedgerData), None, None)

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (Some(batchIssuanceLedgerData))
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (empty)
      }
    }

    "return batch revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(None, Some(batchRevocationLedgerData), None)

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (empty)
        response.batchRevocation mustBe (Some(batchRevocationLedgerData))
        response.credentialRevocation mustBe (empty)
      }
    }

    "return credential revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(None, None, Some(credentialRevocationLedgerData))

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (empty)
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (Some(credentialRevocationLedgerData))
      }
    }

    "return batch issuance data and credential revocation data when the node informs it" in {
      createParticipant("Institution", did)

      val request = console_api.GetLedgerDataRequest(aBatchId, aCredentialHash)

      nodeReturns(Some(batchIssuanceLedgerData), None, Some(credentialRevocationLedgerData))

      usingApiAsCredentials(SignedRpcRequest.generate(keyPair, did, request)) { serviceStub =>
        val response = serviceStub.getLedgerData(request)

        response.batchIssuance mustBe (Some(batchIssuanceLedgerData))
        response.batchRevocation mustBe (empty)
        response.credentialRevocation mustBe (Some(credentialRevocationLedgerData))
      }
    }
  }

  "CredentialsServiceImpl.publishBatch" should {
    def buildSignedIssueCredentialOp(
        credentialHash: SHA256Digest,
        didSuffix: DIDSuffix
    ): node_models.SignedAtalaOperation = {
      node_models.SignedAtalaOperation(
        signedWith = "mockKey",
        signature = ByteString.copyFrom("".getBytes()),
        operation = Some(
          node_models.AtalaOperation(
            operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
              node_models.IssueCredentialBatchOperation(
                credentialBatchData = Some(
                  node_models.CredentialBatchData(
                    issuerDID = didSuffix.value,
                    merkleRoot = ByteString.copyFrom(credentialHash.value.toArray)
                  )
                )
              )
            )
          )
        )
      )
    }

    "forward request to node and store data in database" in {
      val issuerName = "Issuer 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(issuerName, did)

      val mockDIDSuffix = DID.buildPrismDID(SHA256Digest.compute("issuerDIDSuffix".getBytes()).hexValue).suffix
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockEncodedSignedCredentialHash = SHA256Digest.compute(mockEncodedSignedCredential.getBytes())

      val issuanceOp = buildSignedIssueCredentialOp(
        mockEncodedSignedCredentialHash,
        mockDIDSuffix
      )

      val mockCredentialBatchId =
        CredentialBatchId.fromBatchData(mockDIDSuffix, MerkleRoot(mockEncodedSignedCredentialHash))
      val mockTransactionInfo =
        common_models
          .TransactionInfo()
          .withTransactionId(mockCredentialBatchId.id)
          .withLedger(common_models.Ledger.IN_MEMORY)

      val nodeRequest = node_api
        .IssueCredentialBatchRequest()
        .withSignedOperation(issuanceOp)

      nodeMock
        .issueCredentialBatch(nodeRequest)
        .returns(
          Future
            .successful(
              node_api
                .IssueCredentialBatchResponse()
                .withTransactionInfo(mockTransactionInfo)
                .withBatchId(mockCredentialBatchId.id)
            )
        )

      val request = console_api
        .PublishBatchRequest()
        .withIssueCredentialBatchOperation(issuanceOp)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsCredentials(rpcRequest) { serviceStub =>
        val publishResponse = serviceStub.publishBatch(request)
        verify(nodeMock).issueCredentialBatch(nodeRequest)

        publishResponse.batchId mustBe mockCredentialBatchId.id
        publishResponse.transactionInfo mustBe Some(mockTransactionInfo)

        val (txId, ledger, hash) = getBatchData(mockCredentialBatchId).value
        txId.toString mustBe mockTransactionInfo.transactionId
        ledger mustBe (InMemory)
        hash mustBe SHA256Digest.compute(issuanceOp.operation.value.toByteArray)
      }
    }

  }
}
