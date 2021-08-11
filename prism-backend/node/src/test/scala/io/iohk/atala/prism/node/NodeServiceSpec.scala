package io.iohk.atala.prism.node

import cats.effect.IO
import cats.scalatest.EitherMatchers._
import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server, Status, StatusRuntimeException}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DID.masterKeyId
import io.iohk.atala.prism.kotlin.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.{BlockProcessingServiceSpec, ObjectManagementService}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import tofu.syntax.monadic._

import java.time.Instant
import java.util.concurrent.TimeUnit
import io.iohk.atala.prism.protos.node_models.OperationOutput

import scala.concurrent.Future

class NodeServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _

  private val objectManagementService = mock[ObjectManagementService]
  private val credentialBatchesRepository = mock[CredentialBatchesRepository[IO]]

  override def beforeEach(): Unit = {
    super.beforeEach()

    val didDataRepository = DIDDataRepository(database)

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

    service = node_api.NodeServiceGrpc.blockingStub(channelHandle)
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  private val dummyTimestampInfo = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).get,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  private val dummySyncTimestamp = Instant.ofEpochMilli(107)

  "NodeService.getDidDocument" should {
    "return DID document from data in the database" in {
      val didDigest = SHA256Digest.compute("test".getBytes())
      val didSuffix: DIDSuffix = DIDSuffix.fromDigest(didDigest)
      DIDDataDAO.insert(didSuffix, didDigest, dummyLedgerData).transact(database).unsafeRunSync()
      val key = DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, CreateDIDOperationSpec.masterKeys.getPublicKey)
      PublicKeysDAO.insert(key, dummyLedgerData).transact(database).unsafeRunSync()

      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      val response = service.getDidDocument(node_api.GetDidDocumentRequest(s"did:prism:${didSuffix.getValue}"))
      val document = response.document.value
      document.id mustBe didSuffix.getValue
      document.publicKeys.size mustBe 1

      val publicKey = document.publicKeys.headOption.value
      publicKey.id mustBe "master"
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY
      publicKey.addedOn.value mustBe ProtoCodecs.toLedgerData(dummyLedgerData)
      publicKey.revokedOn mustBe empty

      ParsingUtils.parseECKey(ValueAtPath(publicKey.getEcKeyData, Path.root)) must beRight(key.key)

      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }

    "return DID document for an unpublished DID" in {
      val masterKey = CreateDIDOperationSpec.masterKeys.getPublicKey
      val longFormDID = DID.createUnpublishedDID(masterKey, null)
      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      val response = service.getDidDocument(node_api.GetDidDocumentRequest(longFormDID.getValue))
      val document = response.document.value
      document.id mustBe longFormDID.getSuffix.getValue
      document.publicKeys.size mustBe 1

      val publicKey = document.publicKeys.headOption.value
      publicKey.id mustBe masterKeyId
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY
      publicKey.addedOn mustBe empty
      publicKey.revokedOn mustBe empty

      ParsingUtils.parseECKey(ValueAtPath(publicKey.getEcKeyData, Path.root)) must beRight(masterKey)
      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }

    "return DID document for a long form DID after it was published" in {
      val masterKey = CreateDIDOperationSpec.masterKeys.getPublicKey
      val issuingKey = CreateDIDOperationSpec.issuingKeys.getPublicKey
      val longFormDID = DID.createUnpublishedDID(masterKey, null)

      // we simulate the publication of the DID and the addition of an issuing key
      val didDigest = SHA256Digest.fromHex(longFormDID.getCanonicalSuffix.getValue)
      val didSuffix: DIDSuffix = DIDSuffix.fromDigest(didDigest)
      val key1 = DIDPublicKey(didSuffix, masterKeyId, KeyUsage.MasterKey, masterKey)
      val key2 = DIDPublicKey(didSuffix, "issuance0", KeyUsage.IssuingKey, issuingKey)

      (DIDDataDAO.insert(didSuffix, didDigest, dummyLedgerData).transact(database) >>
        PublicKeysDAO.insert(key1, dummyLedgerData).transact(database) >>
        PublicKeysDAO.insert(key2, dummyLedgerData).transact(database)).unsafeRunSync()

      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      // we now resolve the long form DID
      val response = service.getDidDocument(node_api.GetDidDocumentRequest(longFormDID.getValue))
      val document = response.document.value
      document.id mustBe longFormDID.getSuffix.getValue
      document.publicKeys.length mustBe 2

      val publicKey1 = document.publicKeys.find(_.id == masterKeyId).value
      publicKey1.usage mustBe node_models.KeyUsage.MASTER_KEY
      publicKey1.addedOn.value mustBe ProtoCodecs.toLedgerData(dummyLedgerData)
      publicKey1.revokedOn mustBe empty
      ParsingUtils.parseECKey(ValueAtPath(publicKey1.getEcKeyData, Path.root)) must beRight(masterKey)

      val publicKey2 = document.publicKeys.find(_.id == "issuance0").value
      publicKey2.usage mustBe node_models.KeyUsage.ISSUING_KEY
      publicKey2.addedOn.value mustBe ProtoCodecs.toLedgerData(dummyLedgerData)
      publicKey2.revokedOn mustBe empty
      ParsingUtils.parseECKey(ValueAtPath(publicKey2.getEcKeyData, Path.root)) must beRight(issuingKey)

      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }

    "return DID document for a long form DID with revoked key after it was published" in {
      val masterKey = CreateDIDOperationSpec.masterKeys.getPublicKey
      val longFormDID = DID.createUnpublishedDID(masterKey, null)

      // we simulate the publication of the DID and the addition of an issuing key
      val didDigest = SHA256Digest.fromHex(longFormDID.getCanonicalSuffix.getValue)
      val didSuffix = DIDSuffix.fromDigest(didDigest)
      val key = DIDPublicKey(didSuffix, masterKeyId, KeyUsage.MasterKey, masterKey)

      (DIDDataDAO.insert(didSuffix, didDigest, dummyLedgerData).transact(database) >>
        PublicKeysDAO.insert(key, dummyLedgerData).transact(database) >>
        PublicKeysDAO.revoke(key.keyId, dummyLedgerData).transact(database)).unsafeRunSync()

      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      // we now resolve the long form DID
      val response = service.getDidDocument(node_api.GetDidDocumentRequest(longFormDID.getValue))
      val document = response.document.value
      document.id mustBe longFormDID.getSuffix.getValue
      document.publicKeys.length mustBe 1

      val publicKey = document.publicKeys.find(_.id == masterKeyId).value
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY
      publicKey.addedOn.value mustBe ProtoCodecs.toLedgerData(dummyLedgerData)
      publicKey.revokedOn mustBe Some(ProtoCodecs.toLedgerData(dummyLedgerData))
      ParsingUtils.parseECKey(ValueAtPath(publicKey.getEcKeyData, Path.root)) must beRight(masterKey)

      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }
  }

  "NodeService.createDID" should {
    "publish CreateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(operation)

      doReturn(Future.successful(operationId)).when(objectManagementService).publishSingleAtalaOperation(*)

      val response = service.createDID(node_api.CreateDIDRequest().withSignedOperation(operation))

      val expectedDIDSuffix =
        SHA256Digest
          .compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
          .hexValue

      response.id must be(expectedDIDSuffix)
      response.operationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).publishSingleAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation.update(_.createDid.didData.id := "abc"),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.createDID(node_api.CreateDIDRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.updateDID" should {
    "publish UpdateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleOperation,
        "master",
        UpdateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(operation)

      doReturn(Future.successful(operationId)).when(objectManagementService).publishSingleAtalaOperation(*)

      val response = service.updateDID(node_api.UpdateDIDRequest().withSignedOperation(operation))

      response.operationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).publishSingleAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleOperation.update(_.updateDid.id := "abc#@!"),
        "master",
        UpdateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.updateDID(node_api.UpdateDIDRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.issueCredentialBatch" should {
    "publish IssueCredentialBatch operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialBatchOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(operation)

      doReturn(Future.successful(operationId)).when(objectManagementService).publishSingleAtalaOperation(*)

      val response = service.issueCredentialBatch(node_api.IssueCredentialBatchRequest().withSignedOperation(operation))

      val expectedBatchId =
        SHA256Digest
          .compute(
            IssueCredentialBatchOperationSpec.exampleOperation.getIssueCredentialBatch.getCredentialBatchData.toByteArray
          )
          .hexValue

      response.batchId mustBe expectedBatchId
      response.operationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).publishSingleAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialBatchOperationSpec.exampleOperation
          .update(_.issueCredentialBatch.credentialBatchData.merkleRoot := ByteString.copyFrom("abc".getBytes)),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.issueCredentialBatch(node_api.IssueCredentialBatchRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.revokeCredentials" should {
    "publish RevokeCredentials operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialsOperationSpec.revokeFullBatchOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(operation)

      doReturn(Future.successful(operationId)).when(objectManagementService).publishSingleAtalaOperation(*)

      val response = service.revokeCredentials(node_api.RevokeCredentialsRequest().withSignedOperation(operation))

      response.operationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).publishSingleAtalaOperation(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialsOperationSpec.revokeFullBatchOperation.update(_.revokeCredentials.credentialBatchId := ""),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.revokeCredentials(node_api.RevokeCredentialsRequest().withSignedOperation(operation))
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.getBuildInfo" should {
    "return proper build information" in {
      val buildInfo = service.getNodeBuildInfo(GetNodeBuildInfoRequest())

      // This changes greatly, so just test something was set
      buildInfo.version must not be empty
      buildInfo.scalaVersion mustBe "2.13.6"
      buildInfo.sbtVersion mustBe "1.5.3"
    }
  }

  "NodeService.getLastSyncedBlockTimestamp" should {
    "return the timestamp of the last synced block" in {
      doReturn(Future.successful(dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp

      val response = service.getLastSyncedBlockTimestamp(
        GetLastSyncedBlockTimestampRequest()
      )

      response.lastSyncedBlockTimestamp.value must be(dummySyncTimestamp.toProtoTimestamp)
    }
  }

  "NodeService.getOperationInfo" should {
    "return OPERATION_UNKNOWN when operation identifier was not found" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(validOperation)
      val operationIdProto = operationId.toProtoByteString

      doReturn(Future.successful(dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp
      doReturn(Future.successful(None))
        .when(objectManagementService)
        .getOperationInfo(operationId)

      val operationIdRestored = AtalaOperationId.fromVectorUnsafe(operationIdProto.toByteArray.toVector)

      operationId must be(operationIdRestored)

      val response = service.getOperationInfo(
        GetOperationInfoRequest()
          .withOperationId(operationIdProto)
      )

      response.operationStatus must be(common_models.OperationStatus.UNKNOWN_OPERATION)
      response.lastSyncedBlockTimestamp.value must be(dummySyncTimestamp.toProtoTimestamp)
    }

    "return CONFIRM_AND_APPLIED when operation identifier was processed by the node" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val operationId = AtalaOperationId.of(validOperation)
      val operationIdProto = operationId.toProtoByteString
      val operationInfo = AtalaOperationInfo(
        operationId = operationId,
        objectId = AtalaObjectId.of("random".getBytes),
        operationStatus = AtalaOperationStatus.APPLIED,
        transactionSubmissionStatus = Some(AtalaObjectTransactionSubmissionStatus.InLedger),
        transactionId = Some(dummyLedgerData.transactionId)
      )

      doReturn(Future.successful(dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp
      doReturn(Future.successful(Some(operationInfo)))
        .when(objectManagementService)
        .getOperationInfo(operationId)

      val operationIdRestored = AtalaOperationId.fromVectorUnsafe(operationIdProto.toByteArray.toVector)

      operationId must be(operationIdRestored)

      val response = service.getOperationInfo(
        GetOperationInfoRequest()
          .withOperationId(operationIdProto)
      )

      response.operationStatus must be(common_models.OperationStatus.CONFIRMED_AND_APPLIED)
      response.transactionId must be(dummyLedgerData.transactionId.toString)
      response.lastSyncedBlockTimestamp.value must be(dummySyncTimestamp.toProtoTimestamp)
    }
  }

  "NodeService.getBatchState" should {
    "fail when batchId is not valid" in {
      val invalidBatchId = "invalid@_?"
      val requestWithInvalidId = GetBatchStateRequest(batchId = invalidBatchId)
      val expectedMessage = s"INTERNAL: Invalid batch id: $invalidBatchId"

      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      val error = intercept[RuntimeException] {
        service.getBatchState(requestWithInvalidId)
      }
      error.getMessage must be(expectedMessage)
    }

    "return an error when the CredentialBatchesRepository fails" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("valid".getBytes()))
      val requestWithValidId = GetBatchStateRequest(batchId = validBatchId.getId)

      val errorMsg = "an unexpected error"
      val repositoryError =
        IO.raiseError[Either[NodeError, Option[CredentialBatchState]]](new RuntimeException(errorMsg))

      doReturn(repositoryError).when(credentialBatchesRepository).getBatchState(validBatchId)
      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      val err = intercept[RuntimeException](
        service.getBatchState(requestWithValidId)
      )
      err.getMessage.endsWith(errorMsg) must be(true)
    }

    "return empty response when the CredentialBatchesRepository reports no results" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("valid".getBytes()))
      val requestWithValidId = GetBatchStateRequest(batchId = validBatchId.getId)

      val repositoryError = IO.pure[Either[NodeError, Option[CredentialBatchState]]](Right(None))

      doReturn(repositoryError).when(credentialBatchesRepository).getBatchState(validBatchId)
      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp

      val response = service.getBatchState(requestWithValidId)
      response.issuerDid must be("")
      response.merkleRoot must be(empty)
      response.publicationLedgerData must be(empty)
      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }

    "return batch state when CredentialBatchesRepository succeeds" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("valid".getBytes()))
      val requestWithValidId = GetBatchStateRequest(batchId = validBatchId.getId)

      val issuerDIDSuffix: DIDSuffix = DIDSuffix.fromDigest(SHA256Digest.compute("testDID".getBytes()))
      val issuedOnLedgerData = dummyLedgerData
      val merkleRoot = new MerkleRoot(SHA256Digest.compute("content".getBytes()))
      val credState =
        CredentialBatchState(
          merkleRoot = merkleRoot,
          batchId = validBatchId,
          issuerDIDSuffix = issuerDIDSuffix,
          issuedOn = issuedOnLedgerData,
          revokedOn = None,
          lastOperation = SHA256Digest.compute("lastOp".getBytes())
        )

      val repositoryResponse = IO.pure[Either[NodeError, Option[CredentialBatchState]]](Right(Some(credState)))

      val ledgerDataProto = node_models
        .LedgerData()
        .withTransactionId(dummyLedgerData.transactionId.toString)
        .withLedger(common_models.Ledger.IN_MEMORY)
        .withTimestampInfo(
          node_models
            .TimestampInfo()
            .withBlockTimestamp(
              Instant.ofEpochMilli(issuedOnLedgerData.timestampInfo.getAtalaBlockTimestamp).toProtoTimestamp
            )
            .withBlockSequenceNumber(issuedOnLedgerData.timestampInfo.getAtalaBlockSequenceNumber)
            .withOperationSequenceNumber(issuedOnLedgerData.timestampInfo.getOperationSequenceNumber)
        )

      doReturn(
        Future.successful(dummySyncTimestamp)
      ).when(objectManagementService).getLastSyncedTimestamp
      doReturn(repositoryResponse).when(credentialBatchesRepository).getBatchState(validBatchId)

      val response = service.getBatchState(requestWithValidId)
      response.issuerDid must be(issuerDIDSuffix.getValue)
      response.merkleRoot.toByteArray.toVector must be(merkleRoot.getHash.getValue)
      response.publicationLedgerData must be(Some(ledgerDataProto))
      response.revocationLedgerData must be(empty)
      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }
  }

  "NodeService.getCredentialRevocationTime" should {
    "fail when batchId is not valid" in {
      val invalidBatchId = "invalid@_?"
      val validCredentialHash = SHA256Digest.compute("random".getBytes())
      val requestWithInvalidId =
        GetCredentialRevocationTimeRequest(
          batchId = invalidBatchId,
          credentialHash = ByteString.copyFrom(validCredentialHash.getValue)
        )
      val expectedMessage = s"INTERNAL: Invalid batch id: $invalidBatchId"

      doReturn(Future.successful(dummySyncTimestamp)).when(objectManagementService).getLastSyncedTimestamp
      val error = intercept[RuntimeException] {
        service.getCredentialRevocationTime(requestWithInvalidId)
      }
      error.getMessage must be(expectedMessage)
    }

    "fail when credentialHash is not valid" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("random".getBytes()))
      val requestWithInvalidCredentialHash =
        GetCredentialRevocationTimeRequest(
          batchId = validBatchId.getId,
          credentialHash = ByteString.EMPTY
        )

      val expectedMessage =
        "INTERNAL: The given bytes does not correspond to a sha256 hash. It must have exactly 32 bytes"

      doReturn(
        Future.successful(dummySyncTimestamp)
      ).when(objectManagementService).getLastSyncedTimestamp

      val error = intercept[RuntimeException] {
        service.getCredentialRevocationTime(requestWithInvalidCredentialHash)
      }

      error.getMessage must be(expectedMessage)
    }

    "return empty timestamp when CredentialBatchesRepository succeeds returning None" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("valid".getBytes()))
      val validCredentialHash = SHA256Digest.compute("random".getBytes())
      val validRequest = GetCredentialRevocationTimeRequest(
        batchId = validBatchId.getId,
        credentialHash = ByteString.copyFrom(validCredentialHash.getValue)
      )

      val repositoryResponse = IO.pure[Either[NodeError, Option[LedgerData]]](Right(None))

      doReturn(
        Future.successful(dummySyncTimestamp)
      ).when(objectManagementService).getLastSyncedTimestamp

      doReturn(repositoryResponse)
        .when(credentialBatchesRepository)
        .getCredentialRevocationTime(validBatchId, validCredentialHash)

      val response = service.getCredentialRevocationTime(validRequest)
      response.revocationLedgerData must be(empty)
      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }

    "return correct timestamp when CredentialBatchesRepository succeeds returning a time" in {
      val validBatchId = CredentialBatchId.fromDigest(SHA256Digest.compute("valid".getBytes()))
      val validCredentialHash = SHA256Digest.compute("random".getBytes())
      val validRequest = GetCredentialRevocationTimeRequest(
        batchId = validBatchId.getId,
        credentialHash = ByteString.copyFrom(validCredentialHash.getValue)
      )
      val revocationDate = new TimestampInfo(Instant.now().toEpochMilli, 1, 1)
      val revocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(1)).value,
        Ledger.InMemory,
        revocationDate
      )

      val repositoryResponse = IO.pure[Either[NodeError, Option[LedgerData]]](Right(Some(revocationLedgerData)))

      val timestampInfoProto = node_models
        .TimestampInfo()
        .withBlockTimestamp(Instant.ofEpochMilli(revocationDate.getAtalaBlockTimestamp).toProtoTimestamp)
        .withBlockSequenceNumber(revocationDate.getAtalaBlockSequenceNumber)
        .withOperationSequenceNumber(revocationDate.getOperationSequenceNumber)

      val revocationLedgerDataProto = node_models
        .LedgerData()
        .withTransactionId(revocationLedgerData.transactionId.toString)
        .withLedger(common_models.Ledger.IN_MEMORY)
        .withTimestampInfo(timestampInfoProto)

      doReturn(
        Future.successful(dummySyncTimestamp)
      ).when(objectManagementService).getLastSyncedTimestamp

      doReturn(repositoryResponse)
        .when(credentialBatchesRepository)
        .getCredentialRevocationTime(validBatchId, validCredentialHash)

      val response = service.getCredentialRevocationTime(validRequest)
      response.revocationLedgerData must be(Some(revocationLedgerDataProto))
      response.lastSyncedBlockTimestamp must be(Some(dummySyncTimestamp.toProtoTimestamp))
    }
  }

  "NodeService.publishAsABlock" should {
    "fail when called with an empty sequence of operations" in {
      val error = intercept[StatusRuntimeException] {
        service.publishAsABlock(
          node_api
            .PublishAsABlockRequest()
            .withSignedOperations(Seq())
        )
      }

      val expectedMessage = "INTERNAL: requirement failed: there must be at least one operation to be published"
      error.getMessage must be(expectedMessage)
    }

    "return error when at least one provided operation is invalid" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val invalidOperation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialBatchOperationSpec.exampleOperation
          .update(_.issueCredentialBatch.credentialBatchData.merkleRoot := ByteString.copyFrom("abc".getBytes)),
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.publishAsABlock(
          node_api
            .PublishAsABlockRequest()
            .withSignedOperations(Seq(validOperation, invalidOperation))
        )
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
      verifyNoMoreInteractions(objectManagementService)
    }

    "properly return the result of a CreateDID operation and an IssueCredentialBatch operation" in {
      val createDIDOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val createDIDOperationId = AtalaOperationId.of(createDIDOperation)

      val issuanceOperation = BlockProcessingServiceSpec.signOperation(
        IssueCredentialBatchOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val issuanceOperationId = AtalaOperationId.of(issuanceOperation)

      doReturn(Future.successful(List(createDIDOperationId, issuanceOperationId)))
        .when(objectManagementService)
        .publishAtalaOperations(*)

      val response = service.publishAsABlock(
        node_api
          .PublishAsABlockRequest()
          .withSignedOperations(Seq(createDIDOperation, issuanceOperation))
      )

      val expectedBatchId =
        SHA256Digest
          .compute(
            IssueCredentialBatchOperationSpec.exampleOperation.getIssueCredentialBatch.getCredentialBatchData.toByteArray
          )
          .hexValue

      val expectedDIDSuffix =
        SHA256Digest
          .compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
          .hexValue

      response.outputs.size mustBe (2)

      response.outputs.head.getCreateDidOutput.didSuffix mustBe expectedDIDSuffix
      response.outputs.head.operationId mustEqual createDIDOperationId.toProtoByteString

      response.outputs.last.getBatchOutput.batchId mustBe expectedBatchId
      response.outputs.last.operationId mustBe issuanceOperationId.toProtoByteString

      verify(objectManagementService).publishAtalaOperations(createDIDOperation, issuanceOperation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "properly return the result of a CreateDID operation and a DID Update operation" in {
      val createDIDOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val createDIDOperationId = AtalaOperationId.of(createDIDOperation)

      val updateOperation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleOperation,
        "master",
        UpdateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val updateOperationId = AtalaOperationId.of(updateOperation)

      doReturn(Future.successful(List(createDIDOperationId, updateOperationId)))
        .when(objectManagementService)
        .publishAtalaOperations(*)

      val response = service.publishAsABlock(
        node_api
          .PublishAsABlockRequest()
          .withSignedOperations(Seq(createDIDOperation, updateOperation))
      )

      val expectedDIDSuffix =
        SHA256Digest
          .compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
          .hexValue

      response.outputs.size mustBe (2)
      response.outputs.head.getCreateDidOutput.didSuffix mustBe expectedDIDSuffix
      response.outputs.head.operationId mustEqual createDIDOperationId.toProtoByteString

      response.outputs.last.result mustBe OperationOutput.Result.UpdateDidOutput(node_models.UpdateDIDOutput())
      response.outputs.last.operationId mustEqual updateOperationId.toProtoByteString

      verify(objectManagementService).publishAtalaOperations(createDIDOperation, updateOperation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "properly return the result of a RevokeCredentials operation" in {
      val revokeOperation = BlockProcessingServiceSpec.signOperation(
        RevokeCredentialsOperationSpec.revokeFullBatchOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.getPrivateKey
      )
      val revokeOperationId = AtalaOperationId.of(revokeOperation)

      doReturn(Future.successful(List(revokeOperationId))).when(objectManagementService).publishAtalaOperations(*)

      val response = service.publishAsABlock(
        node_api
          .PublishAsABlockRequest()
          .withSignedOperations(Seq(revokeOperation))
      )

      response.outputs.size mustBe (1)
      response.outputs.head.getRevokeCredentialsOutput mustBe node_models.RevokeCredentialsOutput()
      response.outputs.head.operationId mustEqual revokeOperationId.toProtoByteString

      verify(objectManagementService).publishAtalaOperations(revokeOperation)
      verifyNoMoreInteractions(objectManagementService)
    }
  }
}
