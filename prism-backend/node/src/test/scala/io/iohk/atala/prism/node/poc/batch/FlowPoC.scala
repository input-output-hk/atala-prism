package io.iohk.atala.prism.node.poc.batch

import cats.scalatest.ValidatedValues.convertValidatedToValidatable
import com.google.protobuf.ByteString
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.VerificationError.{BatchWasRevoked, CredentialWasRevoked}
import io.iohk.atala.prism.credentials.{Credential, CredentialBatchId, CredentialBatches}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.node.poc.{GenericCredentialsSDK, Wallet}
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, CredentialsRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.services.{
  BlockProcessingServiceImpl,
  CredentialsService,
  DIDDataService,
  InMemoryLedgerService,
  ObjectManagementService
}
import io.iohk.atala.prism.node.{NodeServiceImpl, objects}
import io.iohk.atala.prism.protos.{node_api, node_models}
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.scalatest.BeforeAndAfterEach

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, Promise}

class FlowPoC extends AtalaWithPostgresSpec with BeforeAndAfterEach {
  implicit val ecTrait = EC

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _
  protected var didDataService: DIDDataService = _
  protected var credentialBatchesRepository: CredentialBatchesRepository = _
  protected var credentialsService: CredentialsService = _
  protected var atalaReferenceLedger: InMemoryLedgerService = _
  protected var blockProcessingService: BlockProcessingServiceImpl = _
  protected var objectManagementService: ObjectManagementService = _
  protected var storage: objects.ObjectStorageService = _
  protected var objectManagementServicePromise: Promise[ObjectManagementService] = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    didDataService = new DIDDataService(new DIDDataRepository(database))
    credentialsService = new CredentialsService(new CredentialsRepository(database))
    credentialBatchesRepository = new CredentialBatchesRepository(database)

    storage = new objects.ObjectStorageService.InMemory()

    objectManagementServicePromise = Promise()

    def onAtalaReference(notification: AtalaObjectNotification): Future[Unit] = {
      objectManagementServicePromise.future.futureValue
        .saveObject(notification)
    }

    atalaReferenceLedger = new InMemoryLedgerService(onAtalaReference)
    blockProcessingService = new BlockProcessingServiceImpl
    objectManagementService = ObjectManagementService(
      ObjectManagementService.Config(ledgerPendingTransactionTimeout = Duration.ZERO),
      storage,
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
              didDataService,
              objectManagementService,
              credentialsService,
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

      import FlowPoC._

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
      val signedCreateDIDOp = wallet.signOperation(createDIDOp, "master0", didSuffix)
      connector.registerDID(signedAtalaOperation = signedCreateDIDOp)

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

      val issueBatch1Op = issueBatchOperation(issuerDID, root1)
      val issueBatch2Op = issueBatchOperation(issuerDID, root2)

      val signedIssueBatch1Op = wallet.signOperation(issueBatch1Op, issuanceKeyId, didSuffix)
      val signedIssueBatch2Op = wallet.signOperation(issueBatch2Op, issuanceKeyId, didSuffix)
      console.issueCredentialBatch(signedIssueBatch1Op)
      console.issueCredentialBatch(signedIssueBatch2Op)

      // 7. she encodes the credentials and sends them through the connector along with
      //    the corresponding proofs of inclusion
      val credentialsToSend = signedCredentials.zip(proofs1 ++ proofs2).map {
        case (c, p) =>
          (c.canonicalForm, p)
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
      val issueBatch1OpHash = SHA256Digest.compute(issueBatch1Op.toByteArray)
      val batchId1 = CredentialBatchId.fromBatchData(issuerDID.suffix, root1)
      val revokeBatch1Op = revokeCredentialsOperation(issueBatch1OpHash, batchId1)
      val signedRevokeBatch1Op = wallet.signOperation(revokeBatch1Op, issuanceKeyId, didSuffix)
      console.revokeCredentialBatch(signedRevokeBatch1Op)

      // 11. the issuer decides to revoke the first credential from the second batch
      val issueBatch2OpHash = SHA256Digest.compute(issueBatch2Op.toByteArray)
      val batchId2 = CredentialBatchId.fromBatchData(issuerDID.suffix, root2)
      val revokeC3Op = revokeCredentialsOperation(issueBatch2OpHash, batchId2, Seq(c3.hash))
      val signedRevokeC3Op = wallet.signOperation(revokeC3Op, issuanceKeyId, didSuffix)
      console.revokeSpecificCredentials(signedRevokeC3Op)

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

object FlowPoC {
  def issueBatchOperation(issuerDID: DID, merkleRoot: MerkleRoot): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
          value = node_models
            .IssueCredentialBatchOperation(
              credentialBatchData = Some(
                node_models.CredentialBatchData(
                  issuerDID = issuerDID.suffix.value,
                  merkleRoot = toByteString(merkleRoot.hash)
                )
              )
            )
        )
      )
  }

  def revokeCredentialsOperation(
      previousOperationHash: SHA256Digest,
      batchId: CredentialBatchId,
      credentialsToRevoke: Seq[SHA256Digest] = Nil
  ): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.RevokeCredentials(
          value = node_models
            .RevokeCredentialsOperation(
              previousOperationHash = toByteString(previousOperationHash),
              credentialBatchId = batchId.id,
              credentialsToRevoke = credentialsToRevoke.map(toByteString)
            )
        )
      )
  }

  def toByteString(hash: SHA256Digest): ByteString = ByteString.copyFrom(hash.value.toArray)
}
