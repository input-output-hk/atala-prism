package io.iohk.atala.prism.node.poc.batch

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.scalatest.ValidatedValues.convertValidatedToValidatable
import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.api.CredentialBatches
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.identity.PrismDid.{getDEFAULT_MASTER_KEY_ID => masterKeyId}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.poc.CredVerification.VerificationError._
import io.iohk.atala.prism.node.poc.{GenericCredentialsSDK, Wallet}
import io.iohk.atala.prism.node.repositories._
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.services.{
  BlockProcessingServiceImpl,
  InMemoryLedgerService,
  NodeService,
  ObjectManagementService,
  SubmissionSchedulingService,
  SubmissionService
}
import io.iohk.atala.prism.node.{DataPreparation, NodeGrpcServiceImpl, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.utils.NodeClientUtils._
import org.scalatest.BeforeAndAfterEach
import tofu.logging.Logs

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

class FlowPoC extends AtalaWithPostgresSpec with BeforeAndAfterEach {

  private val flowPocTestLogs = Logs.withContext[IO, IOWithTraceIdContext]
  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _
  protected var didDataRepository: DIDDataRepository[IOWithTraceIdContext] = _
  protected var atalaOperationsRepository: AtalaOperationsRepository[IOWithTraceIdContext] = _
  protected var credentialBatchesRepository: CredentialBatchesRepository[IOWithTraceIdContext] = _
  protected var atalaReferenceLedger: UnderlyingLedger[IOWithTraceIdContext] = _
  protected var blockProcessingService: BlockProcessingServiceImpl = _
  protected var objectManagementService: ObjectManagementService[IOWithTraceIdContext] = _
  protected var submissionService: SubmissionService[IOWithTraceIdContext] = _
  protected var submissionSchedulingService: SubmissionSchedulingService = _
  protected var atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IOWithTraceIdContext] = _
  protected var keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext] =
    _
  protected var protocolVersionsRepository: ProtocolVersionRepository[IOWithTraceIdContext] = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    didDataRepository = DIDDataRepository.unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)
    credentialBatchesRepository = CredentialBatchesRepository.unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)
    protocolVersionsRepository = ProtocolVersionRepository.unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)

    atalaReferenceLedger = InMemoryLedgerService.unsafe(onAtalaReference, flowPocTestLogs)
    blockProcessingService = new BlockProcessingServiceImpl
    atalaOperationsRepository = AtalaOperationsRepository.unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)
    atalaObjectsTransactionsRepository = AtalaObjectsTransactionsRepository
      .unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)
    submissionService = SubmissionService.unsafe(
      atalaReferenceLedger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      logs = flowPocTestLogs
    )
    // this service needs to pull operations from the database and to send them to the ledger
    submissionSchedulingService = SubmissionSchedulingService(
      SubmissionSchedulingService.Config(
        refreshTransactionStatusesPeriod = 1.second,
        operationSubmissionPeriod = 2.second
      ),
      submissionService
    )
    keyValuesRepository = KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, flowPocTestLogs)
    objectManagementService = ObjectManagementService.unsafe(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionsRepository,
      blockProcessingService,
      dbLiftedToTraceIdIO,
      flowPocTestLogs
    )
    def onAtalaReference(
        notification: AtalaObjectNotification
    ): IOWithTraceIdContext[Unit] = objectManagementService
      .saveObject(notification)
      .void

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(
        node_api.NodeServiceGrpc
          .bindService(
            new NodeGrpcServiceImpl(
              NodeService.unsafe(
                didDataRepository,
                objectManagementService,
                credentialBatchesRepository,
                flowPocTestLogs
              )
            ),
            executionContext
          )
      )
      .build()
      .start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()

    nodeServiceStub = node_api.NodeServiceGrpc.blockingStub(channelHandle)
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  "The batch issuance/verification flow" should {
    "work" in {

      // the idea of the flow to implement
      // 1. issuer generates a DID with the wallet
      // 2- she uses the connector to publish it
      // 3. she grabs credential data from the management console
      // 4- she builds 4 generic credentials
      // 5. she signs them with the wallet
      // 6. she issues the credentials as two batches (with 2 credentials per batch)
      //    through the management console
      // 7. she encodes the credentials and sends them through the connector along with
      //    the corresponding proofs of inclusion
      // ... later ...
      // 8. a verifier receives the credentials through the connector
      // 9. gives the signed credentials to the wallet to verify them and it succeeds
      // ... later ...
      // 10. the issuer decides to revoke the first batch
      // 11. the issuer decides to revoke the first credential from the second batch
      // ... later ...
      // 12. the verifier calls the wallet again to verify the credentials
      //     and the verification fails for all but the second credential of the second batch

      val wallet = Wallet(nodeServiceStub)
      val console = ManagementConsole(nodeServiceStub)
      val connector = Connector(nodeServiceStub)

      // 1. issuer generates a DID with the wallet
      val (didSuffix, createDIDOp) = wallet.generateDID()

      // 2- she uses the connector to publish it
      val signedCreateDIDOp =
        wallet.signOperation(createDIDOp, masterKeyId, didSuffix)
      val registerDIDOperationId = connector
        .registerDID(signedAtalaOperation = signedCreateDIDOp)
        .getOperationId
      DataPreparation.waitConfirmation(
        nodeServiceStub,
        registerDIDOperationId
      )

      // 3. she grabs credential data from the management console
      val consoleCredentials = console.getCredentials(4)

      // 4. she builds 4 generic credentials
      val issuanceKeyId = "issuance0"

      val issuerDID = DID.buildCanonical(Sha256Digest.fromHex(didSuffix.value))
      val credentialsToSign = consoleCredentials.map { credential =>
        GenericCredentialsSDK.buildGenericCredential(
          "university-degree",
          issuerDID,
          issuanceKeyId,
          credential.credentialData
        )
      }

      // 5. she signs them with the wallet
      val signedCredentials = credentialsToSign.map { credentialToSign =>
        wallet.signCredential(credentialToSign, issuanceKeyId, didSuffix)
      }

      // 6. she issues the credentials as two batches (with 2 credentials per batch)
      //    through the management console
      val batch1 = CredentialBatches.batch(signedCredentials.take(2).asJava)
      val (root1, proofs1) = (batch1.getRoot, batch1.getProofs.asScala.toList)
      val batch2 = CredentialBatches.batch(signedCredentials.drop(2).asJava)
      val (root2, proofs2) = (batch2.getRoot, batch2.getProofs.asScala.toList)

      val issueBatch1Op = issueBatchOperation(issuerDID, root1)
      val issueBatch2Op = issueBatchOperation(issuerDID, root2)

      val signedIssueBatch1Op =
        wallet.signOperation(issueBatch1Op, issuanceKeyId, didSuffix)
      val signedIssueBatch2Op =
        wallet.signOperation(issueBatch2Op, issuanceKeyId, didSuffix)
      val issueCredentialBatchOperationId1 =
        console.issueCredentialBatch(signedIssueBatch1Op).getOperationId
      val issueCredentialBatchOperationId2 =
        console.issueCredentialBatch(signedIssueBatch2Op).getOperationId
      DataPreparation.waitConfirmation(
        nodeServiceStub,
        issueCredentialBatchOperationId1,
        issueCredentialBatchOperationId2
      )

      // 7. she encodes the credentials and sends them through the connector along with
      //    the corresponding proofs of inclusion
      val credentialsToSend =
        signedCredentials.zip(proofs1 ++ proofs2).map { case (c, p) =>
          (c.getCanonicalForm, p)
        }
      connector.sendCredentialAndProof(credentialsToSend)

      // ... later ...
      // 8. a verifier receives the credentials through the connector
      val List((c1, p1), (c2, p2), (c3, p3), (c4, p4)) =
        connector.receivedCredentialAndProof().map { case (c, p) =>
          (JsonBasedCredential.fromString(c), p)
        }

      // 9. gives the signed credentials to the wallet to verify them and it succeeds
      wallet.verifyCredential(c1, p1).isValid mustBe true
      wallet.verifyCredential(c2, p2).isValid mustBe true
      wallet.verifyCredential(c3, p3).isValid mustBe true
      wallet.verifyCredential(c4, p4).isValid mustBe true

      // ... later ...
      // 10. the issuer decides to revoke the first batch
      val revocationKeyId = "revocation0"
      wallet.addRevocationKeyToDid(
        revocationKeyId = revocationKeyId,
        previousOperationHash = ByteString.copyFrom(Sha256.compute(createDIDOp.toByteArray).getValue),
        didSuffix = didSuffix
      )

      val issueBatch1OpHash = Sha256.compute(issueBatch1Op.toByteArray)
      val batchId1 = CredentialBatchId.fromBatchData(issuerDID.getSuffix, root1)
      val revokeBatch1Op =
        revokeCredentialsOperation(issueBatch1OpHash, batchId1)
      val signedRevokeBatch1Op =
        wallet.signOperation(revokeBatch1Op, revocationKeyId, didSuffix)
      val revokeCredentialBatchOperationId =
        console.revokeCredentialBatch(signedRevokeBatch1Op).getOperationId

      // 11. the issuer decides to revoke the first credential from the second batch
      val issueBatch2OpHash = Sha256.compute(issueBatch2Op.toByteArray)
      val batchId2 = CredentialBatchId.fromBatchData(issuerDID.getSuffix, root2)
      val revokeC3Op =
        revokeCredentialsOperation(issueBatch2OpHash, batchId2, Seq(c3.hash))
      val signedRevokeC3Op =
        wallet.signOperation(revokeC3Op, revocationKeyId, didSuffix)
      val revokeSpecificCredentialsOperationId =
        console.revokeSpecificCredentials(signedRevokeC3Op).getOperationId

      DataPreparation.waitConfirmation(
        nodeServiceStub,
        revokeCredentialBatchOperationId,
        revokeSpecificCredentialsOperationId
      )

      // ... later ...
      // 12. the verifier calls the wallet again to verify the credentials
      //     and the verification fails for all but the second credential of the second batch
      val e1 = wallet.verifyCredential(c1, p1).invalid.e
      e1.size mustBe 1
      e1.head mustBe a[BatchWasRevoked]

      val e2 = wallet.verifyCredential(c2, p2).invalid.e
      e2.size mustBe 1
      e2.head mustBe a[BatchWasRevoked]

      val e3 = wallet.verifyCredential(c3, p3).invalid.e
      e3.size mustBe 1
      e3.head mustBe a[CredentialWasRevoked]

      wallet.verifyCredential(c4, p4).isValid mustBe true
    }
  }
}
