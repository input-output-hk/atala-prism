package io.iohk.node

import java.util.concurrent.TimeUnit

import doobie.implicits._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server, Status, StatusRuntimeException}
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.models.{DIDPublicKey, DIDSuffix, KeyUsage, SHA256Digest}
import io.iohk.node.operations.path.{Path, ValueAtPath}
import io.iohk.node.operations.{
  CreateDIDOperationSpec,
  IssueCredentialOperationSpec,
  ParsingUtils,
  RevokeCredentialOperationSpec
}
import io.iohk.node.repositories.DIDDataRepository
import io.iohk.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.node.services.{BlockProcessingServiceSpec, DIDDataService, ObjectManagementService}
import io.iohk.prism.protos.{node_api, node_models}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

class NodeServiceSpec extends PostgresRepositorySpec with MockitoSugar with BeforeAndAfterEach {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  override val tables: List[String] = List("public_keys", "did_data")

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _

  val objectManagementService = mock[ObjectManagementService]

  override def beforeEach(): Unit = {
    super.beforeEach()

    val didDataService = new DIDDataService(new DIDDataRepository(database))

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(
        node_api.NodeServiceGrpc
          .bindService(new NodeServiceImpl(didDataService, objectManagementService), executionContext)
      )
      .build()
      .start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()

    service = node_api.NodeServiceGrpc.blockingStub(channelHandle)
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  "NodeService.getDidDocument" should {
    "return DID document from data in the database" in {
      val didDigest = SHA256Digest.compute("test".getBytes())
      val didSuffix = DIDSuffix(didDigest)
      DIDDataDAO.insert(didSuffix, didDigest).transact(database).unsafeRunSync()
      val key = DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, CreateDIDOperationSpec.masterKeys.getPublic)
      PublicKeysDAO.insert(key).transact(database).unsafeRunSync()

      val response = service.getDidDocument(node_api.GetDidDocumentRequest(s"did:atala:${didSuffix.suffix}"))
      val document = response.document.value
      document.id mustBe didSuffix.suffix
      document.publicKeys.size mustBe 1

      val publicKey = document.publicKeys.headOption.value
      publicKey.id mustBe "master"
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY

      ParsingUtils.parseECKey(ValueAtPath(publicKey.getEcKeyData, Path.root)).right.value mustBe key.key
    }
  }

  "NodeService.createDID" should {
    "publish CreateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      doReturn(Future.successful(())).when(objectManagementService).publishAtalaOperation(*)

      service.createDID(operation)

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation.update(_.createDid.didData.id := "abc"),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      val error = intercept[StatusRuntimeException] {
        service.createDID(operation)
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.issueCredential" should {
    "publish IssueCredential operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      doReturn(Future.successful(())).when(objectManagementService).publishAtalaOperation(*)

      service.issueCredential(operation)

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialOperationSpec.exampleOperation.update(_.issueCredential.credentialData.id := "abc"),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      val error = intercept[StatusRuntimeException] {
        service.issueCredential(operation)
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.revokeCredential" should {
    "publish RevokeCredential operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      doReturn(Future.successful(())).when(objectManagementService).publishAtalaOperation(*)

      service.revokeCredential(operation)

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialOperationSpec.exampleOperation.update(_.revokeCredential.credentialId := ""),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivate
      )

      val error = intercept[StatusRuntimeException] {
        service.revokeCredential(operation)
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }
}
