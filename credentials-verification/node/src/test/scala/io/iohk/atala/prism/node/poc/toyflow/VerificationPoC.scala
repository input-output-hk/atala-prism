package io.iohk.atala.prism.node.poc.toyflow

import java.time.Duration
import java.util.concurrent.TimeUnit

import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.node.poc.NodeSDK
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
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import io.iohk.atala.prism.credentials.json.implicits._

class VerificationPoC extends PostgresRepositorySpec with BeforeAndAfterEach {
  implicit val ecTrait = EC
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

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
            ec
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

  "VerificationPoC" should {
    "Work with a toy credential" in {

      // the idea of the flow to implement
      // 1. issuer generates a DID with the wallet
      // 2- she uses the connector to publish it
      // 3. she grabs credential data from the CManager
      // 4- she builds a generic credential
      // 5. she signs it with the wallet
      // 6. she issues the credential through the cmanager
      // 7. she encodes the credential and send it through the connector
      // ... later ...
      // 8. a verifier receives the credential through the connector
      // 9. gives the signed credential to the wallet to verify it and succeeds
      // ... later ...
      // 10. the issuer decides to revoke the credential
      // ... later ...
      // 11. the verifier calls the wallet again to verify the credential
      //     and the verification fails

      // some illustrative component representations
      val connector = Connector(nodeServiceStub)
      val wallet = Wallet(nodeServiceStub)
      val cmanager = CManager(nodeServiceStub)

      // 1. issuer generates a DID with the wallet
      val (didSuffix, createDIDOp) = wallet.generateDID()

      // 2- she uses the connector to publish it
      val signedCreateDIDOp = wallet.signOperation(createDIDOp, "master0", didSuffix)
      val suffixReturned = connector.registerDID(signedAtalaOperation = signedCreateDIDOp)

      suffixReturned must be(didSuffix.suffix)

      // 3. she grabs credential data from the CManager
      val cmanagerCredential = cmanager.getCredential()

      // 4- she builds a generic credential
      val issuanceKeyId = "issuance0"

      val credentialToSign =
        GenericCredentialsSDK.buildGenericCredential(
          "university-degree",
          didSuffix,
          issuanceKeyId,
          cmanagerCredential.credentialData
        )

      // 5. she signs it with the wallet
      val signedCredential = wallet.signCredential(credentialToSign, issuanceKeyId, didSuffix)

      // 6. she issues the credential through the cmanager
      // we create the issuance operation
      val issueCredentialOp = NodeSDK.buildIssueCredentialOp(signedCredential.hash, didSuffix)
      val calculatedCredentialId = NodeSDK.computeCredId(issueCredentialOp)
      val signedIssueCredentialOp = wallet.signOperation(issueCredentialOp, issuanceKeyId, didSuffix)

      val credentialIdReturned = cmanager.issueCredential("some adequate id", signedIssueCredentialOp)

      credentialIdReturned must be(calculatedCredentialId.id)

      // 7. she sends the credential through the connector
      connector.sendCredential(signedCredential.canonicalForm)

      // ... later ...
      // 8. a verifier receives the credential through the connector
      val verifyMe = JsonBasedCredential.unsafeFromString(connector.receivedCredential())

      // 9. he gives the signed credential to the wallet to verify it
      //    and the wallet queries the node and verify stuff and succeeds
      val validResult = wallet.verifyCredential(verifyMe)

      validResult must be(true)

      // ... later ...
      // 10. the issuer decides to revoke the credential
      val prevOpHash: SHA256Digest = SHA256Digest.compute(issueCredentialOp.toByteArray)
      val credentialIdToRevoke = calculatedCredentialId
      val revokeCredOp = NodeSDK.buildRevokeCredentialOp(prevOpHash, credentialIdToRevoke)
      val signedRevokeOp = wallet.signOperation(revokeCredOp, issuanceKeyId, didSuffix)
      cmanager.revokeCredential("the corresponding id", signedRevokeOp)

      // ... later ...
      // 11. the verifier calls the wallet again to verify the credential
      //     and the verification fails
      val invalidResult = wallet.verifyCredential(verifyMe)

      invalidResult must be(false)
    }
  }
}
