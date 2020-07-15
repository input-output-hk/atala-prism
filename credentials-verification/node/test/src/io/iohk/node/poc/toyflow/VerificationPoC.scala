package io.iohk.node.poc.toyflow

import java.time.Instant
import java.util.concurrent.TimeUnit

import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.crypto.poc.{CryptoSDKImpl, SignedCredential}
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.poc.{CManager, Connector, NodeSDK}
import io.iohk.node.repositories.{CredentialsRepository, DIDDataRepository}
import io.iohk.node.services.models.AtalaObjectUpdate
import io.iohk.node.services.{BlockProcessingServiceImpl, CredentialsService, DIDDataService, ObjectManagementService}
import io.iohk.node.{InMemoryAtalaReferenceLedger, NodeServiceImpl, objects}
import io.iohk.prism.protos.node_api
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class VerificationPoC extends PostgresRepositorySpec with MockitoSugar with BeforeAndAfterEach {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _
  protected var didDataService: DIDDataService = _
  protected var credentialsService: CredentialsService = _
  protected var atalaReferenceLedger: InMemoryAtalaReferenceLedger = _
  protected var blockProcessingService: BlockProcessingServiceImpl = _
  protected var objectManagementService: ObjectManagementService = _
  protected var storage: objects.ObjectStorageService = _
  protected var objectManagementServicePromise: Promise[ObjectManagementService] = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    didDataService = new DIDDataService(new DIDDataRepository(database))
    credentialsService = new CredentialsService(new CredentialsRepository(database))

    storage = new objects.ObjectStorageService.InMemory()

    objectManagementServicePromise = Promise()

    def onAtalaReference(ref: AtalaObjectUpdate, timestamp: Instant): Future[Unit] = {
      objectManagementServicePromise.future.futureValue
        .saveObject(ref, timestamp)
    }

    atalaReferenceLedger = new InMemoryAtalaReferenceLedger(onAtalaReference)
    blockProcessingService = new BlockProcessingServiceImpl
    objectManagementService = new ObjectManagementService(storage, atalaReferenceLedger, blockProcessingService)
    objectManagementServicePromise.success(objectManagementService)

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(
        node_api.NodeServiceGrpc
          .bindService(
            new NodeServiceImpl(didDataService, objectManagementService, credentialsService),
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

  "VerificationPoC" should {
    "Work with a toy credential" in {

      // the idea of the flow to implement
      // 1. issuer generates a DID with the wallet
      // 2- she uses the connector to publish it
      // 3. she grabs credential data from the CManager
      // 4- she builds a generic credential
      // 5. she signes it with the wallet
      // 6. she issues the credential through the cmanager
      // /. she encode the credential and send it through the connector
      // ... later ...
      // 8. a verifier recieves the credential through the connector
      // 9. gives the signed credential to the wallet to verify it
      // 10. the wallet queries the node and verify stuff and succeeds
      // ... later ...
      // 11. the issuer decides to revoke the crdential
      // ... later ...
      // 12. the verifier calls the wallet again to verify the credential
      //     and the verification fails

      // some illustrative component representations
      val connector = Connector(nodeServiceStub)
      val wallet = Wallet(nodeServiceStub)
      val cmanager = CManager(nodeServiceStub)
      val cryptoSDK = CryptoSDKImpl

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
      val hash = cryptoSDK.hash(signedCredential)
      val issueCredentialOp = NodeSDK.buildIssueCredentialOp(hash, didSuffix)
      val calculatedCredentialId = NodeSDK.computeCredId(issueCredentialOp)
      val signedIssueCredentialOp = wallet.signOperation(issueCredentialOp, issuanceKeyId, didSuffix)

      val credentialIdReturned = cmanager.issueCredential("some adequate id", signedIssueCredentialOp)

      credentialIdReturned must be(calculatedCredentialId.id)

      // 7. she sends the credential through the connector
      connector.sendCredential(signedCredential.canonicalForm)

      // ... later ...
      // 8. a verifier receives the credential through the connector
      val verifyMe = SignedCredential.from(connector.receivedCredential()).get

      // 9. he gives the signed credential to the wallet to verify it
      //    and the wallet queries the node and verify stuff and succeeds
      val validResult = wallet.verifyCredential(verifyMe)

      validResult must be(true)

      // ... later ...
      // 11. the issuer decides to revoke the credential
      val prevOpHash: SHA256Digest = SHA256Digest.compute(issueCredentialOp.toByteArray)
      val credentialIdToRevoke = calculatedCredentialId
      val revokeCredOp = NodeSDK.buildRevokeCredentialOp(prevOpHash, credentialIdToRevoke)
      val signedRevokeOp = wallet.signOperation(revokeCredOp, issuanceKeyId, didSuffix)
      cmanager.revokeCredential("the corresponding id", signedRevokeOp)

      // ... later ...
      // 12. the verifier calls the wallet again to verify the credential
      //     and the verification fails
      val invalidResult = wallet.verifyCredential(verifyMe)

      invalidResult must be(false)
    }
  }
}
