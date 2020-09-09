package io.iohk.atala.prism.node

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import doobie.implicits._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server, Status, StatusRuntimeException}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.models.{
  CredentialId,
  DIDPublicKey,
  DIDSuffix,
  KeyUsage,
  Ledger,
  TransactionId,
  TransactionInfo
}
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.operations.{
  CreateDIDOperationSpec,
  IssueCredentialOperationSpec,
  ParsingUtils,
  RevokeCredentialOperationSpec,
  TimestampInfo
}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.node.services.{
  BlockProcessingServiceSpec,
  CredentialsService,
  DIDDataService,
  ObjectManagementService
}
import io.iohk.prism.protos.node_api.{GetCredentialStateRequest, GetNodeBuildInfoRequest}
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

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _

  val objectManagementService = mock[ObjectManagementService]
  val credentialsService = mock[CredentialsService]

  private val testTransactionInfo =
    TransactionInfo(TransactionId.from(SHA256Digest.compute("test".getBytes()).value).value, Ledger.InMemory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    val didDataService = new DIDDataService(new DIDDataRepository(database))

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
      val dummyTime = TimestampInfo.dummyTime
      DIDDataDAO.insert(didSuffix, didDigest).transact(database).unsafeRunSync()
      val key = DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, CreateDIDOperationSpec.masterKeys.publicKey)
      PublicKeysDAO.insert(key, dummyTime).transact(database).unsafeRunSync()

      val response = service.getDidDocument(node_api.GetDidDocumentRequest(s"did:prism:${didSuffix.suffix}"))
      val document = response.document.value
      document.id mustBe didSuffix.suffix
      document.publicKeys.size mustBe 1

      val publicKey = document.publicKeys.headOption.value
      publicKey.id mustBe "master"
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY
      ProtoCodecs.fromTimestampInfoProto(publicKey.addedOn.value) mustBe dummyTime
      publicKey.revokedOn mustBe empty

      ParsingUtils.parseECKey(ValueAtPath(publicKey.getEcKeyData, Path.root)).right.value mustBe key.key
    }
  }

  "NodeService.createDID" should {
    "publish CreateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      doReturn(Future.successful(testTransactionInfo)).when(objectManagementService).publishAtalaOperation(*)

      service.createDID(node_api.CreateDIDRequest().withSignedOperation(operation))

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation.update(_.createDid.didData.id := "abc"),
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.createDID(node_api.CreateDIDRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.issueCredential" should {
    "publish IssueCredential operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      doReturn(Future.successful(testTransactionInfo)).when(objectManagementService).publishAtalaOperation(*)

      service.issueCredential(node_api.IssueCredentialRequest().withSignedOperation(operation))

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialOperationSpec.exampleOperation.update(_.issueCredential.credentialData.id := "abc"),
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.issueCredential(node_api.IssueCredentialRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.revokeCredential" should {
    "publish RevokeCredential operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      doReturn(Future.successful(testTransactionInfo)).when(objectManagementService).publishAtalaOperation(*)

      service.revokeCredential(node_api.RevokeCredentialRequest().withSignedOperation(operation))

      verify(objectManagementService).publishAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialOperationSpec.exampleOperation.update(_.revokeCredential.credentialId := ""),
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.revokeCredential(node_api.RevokeCredentialRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.getBuildInfo" should {
    "return proper build information" in {
      // Use a month so that's long enough to not cache the build date but short enough to be helpful for the test
      val aMonthAgo = LocalDateTime.now(ZoneOffset.UTC).minusMonths(1)

      val buildInfo = service.getNodeBuildInfo(GetNodeBuildInfoRequest())

      // This changes greatly, so just test something was set
      buildInfo.version must not be empty
      buildInfo.scalaVersion mustBe "2.12.10"
      buildInfo.millVersion mustBe "0.6.2"
      // Give it enough time between build creation and test
      val buildTime = LocalDateTime.parse(buildInfo.buildTime)
      buildTime.compareTo(aMonthAgo) must be > 0
    }
  }

  "NodeService.getCredentialState" should {
    "fail when credentialId is not valid" in {
      val requestWithInvalidId = GetCredentialStateRequest(credentialId = "")
      val ex = new RuntimeException("INTERNAL: requirement failed")

      val error = intercept[RuntimeException] {
        service.getCredentialState(requestWithInvalidId)
      }
      error.getMessage must be(ex.getMessage)
    }

    "fail when the CredentialService reports an error" in {
      val validCredentialId = CredentialId(SHA256Digest.compute("valid".getBytes()))
      val requestWithValidId = GetCredentialStateRequest(credentialId = validCredentialId.id)
      val ex = new RuntimeException(s"UNKNOWN: Unknown credential_id: ${validCredentialId.id}")

      val repositoryError = new FutureEither[NodeError, CredentialState](
        Future(
          Left(UnknownValueError("credential_id", validCredentialId.id))
        )
      )

      doReturn(repositoryError).when(credentialsService).getCredentialState(validCredentialId)

      val serviceError = intercept[RuntimeException] {
        service.getCredentialState(requestWithValidId)
      }
      serviceError.getMessage must be(ex.getMessage)
    }

    "return credential state when CredentialService succeeds" in {
      val validCredentialId = CredentialId(SHA256Digest.compute("valid".getBytes()))
      val requestWithValidId = GetCredentialStateRequest(credentialId = validCredentialId.id)

      val issuerDIDSuffix = DIDSuffix(SHA256Digest.compute("testDID".getBytes()))
      val issuedOn = TimestampInfo.dummyTime
      val credState =
        CredentialState(
          contentHash = SHA256Digest.compute("content".getBytes()),
          credentialId = validCredentialId,
          issuerDIDSuffix = issuerDIDSuffix,
          issuedOn = issuedOn,
          revokedOn = None,
          lastOperation = SHA256Digest.compute("lastOp".getBytes())
        )

      val repositoryResponse = new FutureEither[NodeError, CredentialState](
        Future(
          Right(credState)
        )
      )

      val timestampInfoProto = node_models
        .TimestampInfo()
        .withBlockTimestamp(issuedOn.atalaBlockTimestamp.toEpochMilli)
        .withBlockSequenceNumber(issuedOn.atalaBlockSequenceNumber)
        .withOperationSequenceNumber(issuedOn.operationSequenceNumber)

      doReturn(repositoryResponse).when(credentialsService).getCredentialState(validCredentialId)

      val response = service.getCredentialState(requestWithValidId)
      response.issuerDID must be(issuerDIDSuffix.suffix)
      response.publicationDate must be(Some(timestampInfoProto))
      response.revocationDate must be(empty)
    }
  }
}
