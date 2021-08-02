package io.iohk.atala.prism.node.poc.batch

import cats.effect.IO
import cats.scalatest.ValidatedValues.convertValidatedToValidatable
import com.google.protobuf.ByteString
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.VerificationError.{BatchWasRevoked, CredentialWasRevoked}
import io.iohk.atala.prism.credentials.{Credential, CredentialBatchId, CredentialBatches}
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.crypto.{EC => ECScalaSDK}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.masterKeyId
import io.iohk.atala.prism.node.poc.{GenericCredentialsSDK, Wallet}
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.services.{BlockProcessingServiceImpl, InMemoryLedgerService, ObjectManagementService}
import io.iohk.atala.prism.node.{DataPreparation, NodeServiceImpl}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.services.NodeClientService.{issueBatchOperation, revokeCredentialsOperation}
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.scalatest.BeforeAndAfterEach

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, Promise}

import io.iohk.atala.prism.interop.toKotlinSDK._

class FlowPoC extends AtalaWithPostgresSpec with BeforeAndAfterEach {
  implicit val ecTrait = ECScalaSDK

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _
  protected var didDataRepository: DIDDataRepository[IO] = _
  protected var credentialBatchesRepository: CredentialBatchesRepository[IO] = _
  protected var atalaReferenceLedger: InMemoryLedgerService = _
  protected var blockProcessingService: BlockProcessingServiceImpl = _
  protected var objectManagementService: ObjectManagementService = _
  protected var objectManagementServicePromise: Promise[ObjectManagementService] = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    didDataRepository = DIDDataRepository(database)
    credentialBatchesRepository = CredentialBatchesRepository(database)

    objectManagementServicePromise = Promise()

    def onAtalaReference(notification: AtalaObjectNotification): Future[Unit] = {
      objectManagementServicePromise.future.futureValue
        .saveObject(notification)
    }

    atalaReferenceLedger = new InMemoryLedgerService(onAtalaReference)
    blockProcessingService = new BlockProcessingServiceImpl
    objectManagementService = ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      atalaReferenceLedger,
      blockProcessingService
    )
    objectManagementServicePromise.success(objectManagementService)

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(
        node_api.NodeServiceGrpc
          .bindService(
            new NodeServiceImpl(
              didDataRepository,
              objectManagementService,
              credentialBatchesRepository
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
      val signedCreateDIDOp = wallet.signOperation(createDIDOp, masterKeyId, didSuffix)
      val registerDIDOperationId = connector
        .registerDID(signedAtalaOperation = signedCreateDIDOp)
        .operationId
      DataPreparation.flushOperationsAndWaitConfirmation(
        nodeServiceStub,
        registerDIDOperationId
      )

      // 3. she grabs credential data from the management console
      val consoleCredentials = console.getCredentials(4)

      // 4. she builds 4 generic credentials
      val issuanceKeyId = "issuance0"

      val issuerDID = DID.buildPrismDID(didSuffix)
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
      val (root1, proofs1) = CredentialBatches.batch(signedCredentials.take(2))
      val (root2, proofs2) = CredentialBatches.batch(signedCredentials.drop(2))

      val issueBatch1Op = issueBatchOperation(issuerDID, root1.asKotlin)
      val issueBatch2Op = issueBatchOperation(issuerDID, root2.asKotlin)

      val signedIssueBatch1Op = wallet.signOperation(issueBatch1Op, issuanceKeyId, didSuffix)
      val signedIssueBatch2Op = wallet.signOperation(issueBatch2Op, issuanceKeyId, didSuffix)
      val issueCredentialBatchOperationId1 = console.issueCredentialBatch(signedIssueBatch1Op).operationId
      val issueCredentialBatchOperationId2 = console.issueCredentialBatch(signedIssueBatch2Op).operationId
      DataPreparation.flushOperationsAndWaitConfirmation(
        nodeServiceStub,
        issueCredentialBatchOperationId1,
        issueCredentialBatchOperationId2
      )

      // 7. she encodes the credentials and sends them through the connector along with
      //    the corresponding proofs of inclusion
      val credentialsToSend = signedCredentials.zip(proofs1 ++ proofs2).map {
        case (c, p) =>
          (c.canonicalForm, p.asKotlin)
      }
      connector.sendCredentialAndProof(credentialsToSend)

      // ... later ...
      // 8. a verifier receives the credentials through the connector
      val List((c1, p1), (c2, p2), (c3, p3), (c4, p4)) = connector.receivedCredentialAndProof().map {
        case (c, p) =>
          (Credential.unsafeFromString(c), p)
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
        previousOperationHash = ByteString.copyFrom(SHA256Digest.compute(createDIDOp.toByteArray).getValue),
        didSuffix = didSuffix
      )

      val issueBatch1OpHash = SHA256Digest.compute(issueBatch1Op.toByteArray)
      val batchId1 = CredentialBatchId.fromBatchData(issuerDID.suffix, root1)
      val revokeBatch1Op = revokeCredentialsOperation(issueBatch1OpHash, batchId1)
      val signedRevokeBatch1Op = wallet.signOperation(revokeBatch1Op, revocationKeyId, didSuffix)
      val revokeCredentialBatchOperationId = console.revokeCredentialBatch(signedRevokeBatch1Op).operationId

      // 11. the issuer decides to revoke the first credential from the second batch
      val issueBatch2OpHash = SHA256Digest.compute(issueBatch2Op.toByteArray)
      val batchId2 = CredentialBatchId.fromBatchData(issuerDID.suffix, root2)
      val revokeC3Op = revokeCredentialsOperation(issueBatch2OpHash, batchId2, Seq(c3.hash.asKotlin))
      val signedRevokeC3Op = wallet.signOperation(revokeC3Op, revocationKeyId, didSuffix)
      val revokeSpecificCredentialsOperationId = console.revokeSpecificCredentials(signedRevokeC3Op).operationId

      DataPreparation.flushOperationsAndWaitConfirmation(
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
