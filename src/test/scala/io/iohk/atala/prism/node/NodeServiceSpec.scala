package io.iohk.atala.prism.node

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.scalatest.EitherMatchers._
import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server, Status, StatusRuntimeException}
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.models.{AtalaOperationId, DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.node.services.{BlockProcessingServiceSpec, NodeService, ObjectManagementService}
import io.iohk.atala.prism.node.models.TimestampInfo
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_api.OperationOutput
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.node.utils.IOUtils._
import io.iohk.atala.prism.node.utils.syntax._
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import tofu.logging.Logs

import java.time.Instant
import java.util.concurrent.TimeUnit

class NodeServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _

  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val objectManagementService =
    mock[ObjectManagementService[IOWithTraceIdContext]]

  def fake[T](a: T): ReaderT[IO, TraceId, T] =
    ReaderT.apply[IO, TraceId, T](_ => IO.pure(a))

  override def beforeEach(): Unit = {
    super.beforeEach()

    val didDataRepository = DIDDataRepository.unsafe(dbLiftedToTraceIdIO, logs)

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
                logs
              )
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

  private val dummyTimestampInfo =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .get,
    Ledger.InMemory,
    dummyTimestampInfo
  )

  private val dummySyncTimestamp = Instant.ofEpochMilli(107)

  private def mockOperationId(operationId: AtalaOperationId) =
    doReturn(fake[List[Either[NodeError, AtalaOperationId]]](List(Right(operationId))))
      .when(objectManagementService)
      .scheduleAtalaOperations(*)

  "NodeService.getDidDocument" should {
    "return DID document from data in the database" in {
      val didDigest = Sha256Hash.compute("test".getBytes())
      val didSuffix: DidSuffix = DidSuffix(didDigest.hexEncoded)
      DIDDataDAO
        .insert(didSuffix, didDigest, dummyLedgerData)
        .transact(database)
        .unsafeRunSync()
      val key = DIDPublicKey(
        didSuffix,
        "master",
        KeyUsage.MasterKey,
        CryptoTestUtils.toPublicKeyData(CreateDIDOperationSpec.masterKeys.publicKey)
      )
      PublicKeysDAO
        .insert(key, dummyLedgerData)
        .transact(database)
        .unsafeRunSync()

      doReturn(fake[Instant](dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp

      val response = service.getDidDocument(
        node_api.GetDidDocumentRequest(s"did:prism:${didSuffix.getValue}")
      )
      val document = response.document.value
      document.id mustBe didSuffix.getValue
      document.publicKeys.size mustBe 1

      val publicKey = document.publicKeys.headOption.value
      publicKey.id mustBe "master"
      publicKey.usage mustBe node_models.KeyUsage.MASTER_KEY
      publicKey.addedOn.value mustBe ProtoCodecs.toLedgerData(dummyLedgerData)
      publicKey.revokedOn mustBe empty

      ParsingUtils
        .parseCompressedECKey(
          ValueAtPath(publicKey.getCompressedEcKeyData, Path.root)
        )
        .map(_.compressedKey.toVector) must beRight(key.key.compressedKey.toVector)

      response.lastSyncedBlockTimestamp must be(
        Some(dummySyncTimestamp.toProtoTimestamp)
      )

      response.lastUpdateOperation must be(
        ByteString.copyFrom(didDigest.bytes.toArray)
      )
    }

    "return error for a long form DID" in {
      val masterKey = CreateDIDOperationSpec.masterKeys.publicKey
      val longFormDID = DID.buildLongFormFromMasterPublicKey(masterKey)
      doReturn(fake[Instant](dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp

      val error = intercept[StatusRuntimeException] {
        service.getDidDocument(
          node_api.GetDidDocumentRequest(longFormDID.value)
        )
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.createDID" should {
    "schedule CreateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(operation)
      mockOperationId(operationId)

      val response = service
        .scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
        .outputs
        .head

      val expectedDIDSuffix =
        Sha256Hash
          .compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
          .hexEncoded

      response.getCreateDidOutput.didSuffix must be(expectedDIDSuffix)
      response.getOperationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).scheduleAtalaOperations(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "schedule CreateDID operation with compressed keys" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperationWithCompressedKeys,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(operation)
      mockOperationId(operationId)

      val response = service
        .scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
        .outputs
        .head

      val expectedDIDSuffix =
        Sha256Hash
          .compute(
            CreateDIDOperationSpec.exampleOperationWithCompressedKeys.toByteArray
          )
          .hexEncoded

      response.getCreateDidOutput.didSuffix must be(expectedDIDSuffix)
      response.getOperationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).scheduleAtalaOperations(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation.update(
          _.updateDid.actions(0).addKey.key.id := ""
        ),
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.updateDID" should {
    "schedule UpdateDID operation" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation,
        "master",
        UpdateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(operation)
      mockOperationId(operationId)

      val response = service
        .scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
        .outputs
        .head

      response.getOperationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).scheduleAtalaOperations(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "schedule UpdateDID operation with compressed keys" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleOperationWithCompressedKeys,
        "master",
        UpdateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(operation)
      mockOperationId(operationId)

      val response = service
        .scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
        .outputs
        .head

      response.getOperationId mustEqual operationId.toProtoByteString
      verify(objectManagementService).scheduleAtalaOperations(operation)
      verifyNoMoreInteractions(objectManagementService)
    }

    "return error when provided operation is invalid" in {
      val operation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation.update(
          _.updateDid.id := "abc#@!"
        ),
        "master",
        UpdateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.scheduleOperations(
          node_api.ScheduleOperationsRequest(List(operation))
        )
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
    }
  }

  "NodeService.getBuildInfo" should {
    "return proper build and protocol information" in {
      val currentNetworkProtocolMajorVersion = 2
      val currentNetworkProtocolMinorVersion = 5

      doReturn(
        fake[ProtocolVersion](ProtocolVersion(currentNetworkProtocolMajorVersion, currentNetworkProtocolMinorVersion))
      ).when(objectManagementService).getCurrentProtocolVersion

      val buildInfo = service.getNodeBuildInfo(GetNodeBuildInfoRequest())

      // This changes greatly, so just test something was set
      buildInfo.version must not be empty
      buildInfo.scalaVersion mustBe "2.13.15"
      buildInfo.sbtVersion mustBe "1.10.2"
    }
  }

  "return proper protocol information" in {
    val currentNetworkProtocolMajorVersion = 2
    val currentNetworkProtocolMinorVersion = 5

    doReturn(
      fake[ProtocolVersion](ProtocolVersion(currentNetworkProtocolMajorVersion, currentNetworkProtocolMinorVersion))
    ).when(objectManagementService).getCurrentProtocolVersion

    val nodeNetworkProtocolInfo = service.getNodeNetworkProtocolInfo(GetNodeNetworkProtocolInfoRequest())

    nodeNetworkProtocolInfo.supportedNetworkProtocolVersion.map(_.majorVersion) mustBe Some(1)
    nodeNetworkProtocolInfo.supportedNetworkProtocolVersion.map(_.minorVersion) mustBe Some(0)
    nodeNetworkProtocolInfo.currentNetworkProtocolVersion.map(_.majorVersion) mustBe Some(
      currentNetworkProtocolMajorVersion
    )
    nodeNetworkProtocolInfo.currentNetworkProtocolVersion.map(_.minorVersion) mustBe Some(
      currentNetworkProtocolMinorVersion
    )
  }

  "NodeService.getLastSyncedBlockTimestamp" should {
    "return the timestamp of the last synced block" in {
      doReturn(fake[Instant](dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp

      val response = service.getLastSyncedBlockTimestamp(
        GetLastSyncedBlockTimestampRequest()
      )

      response.lastSyncedBlockTimestamp.value must be(
        dummySyncTimestamp.toProtoTimestamp
      )
    }
  }

  "NodeService.getOperationInfo" should {
    "return OPERATION_UNKNOWN when operation identifier was not found" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(validOperation)
      val operationIdProto = operationId.toProtoByteString

      doReturn(fake[Instant](dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp
      doReturn(fake[Option[AtalaOperationInfo]](None))
        .when(objectManagementService)
        .getOperationInfo(operationId)

      val operationIdRestored =
        AtalaOperationId.fromVectorUnsafe(operationIdProto.toByteArray.toVector)

      operationId must be(operationIdRestored)

      val response = service.getOperationInfo(
        GetOperationInfoRequest()
          .withOperationId(operationIdProto)
      )

      response.operationStatus must be(
        common_models.OperationStatus.UNKNOWN_OPERATION
      )
      response.lastSyncedBlockTimestamp.value must be(
        dummySyncTimestamp.toProtoTimestamp
      )
    }

    "return CONFIRM_AND_APPLIED when operation identifier was processed by the node" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
      val operationId = AtalaOperationId.of(validOperation)
      val operationIdProto = operationId.toProtoByteString
      val operationInfo = AtalaOperationInfo(
        operationId = operationId,
        objectId = AtalaObjectId.of("random".getBytes),
        operationStatus = AtalaOperationStatus.APPLIED,
        "",
        transactionSubmissionStatus = Some(AtalaObjectTransactionSubmissionStatus.InLedger),
        transactionId = Some(dummyLedgerData.transactionId)
      )

      doReturn(fake[Instant](dummySyncTimestamp))
        .when(objectManagementService)
        .getLastSyncedTimestamp
      doReturn(fake[Option[AtalaOperationInfo]](Some(operationInfo)))
        .when(objectManagementService)
        .getOperationInfo(operationId)

      val operationIdRestored =
        AtalaOperationId.fromVectorUnsafe(operationIdProto.toByteArray.toVector)

      operationId must be(operationIdRestored)

      val response = service.getOperationInfo(
        GetOperationInfoRequest()
          .withOperationId(operationIdProto)
      )

      response.operationStatus must be(
        common_models.OperationStatus.CONFIRMED_AND_APPLIED
      )
      response.transactionId must be(dummyLedgerData.transactionId.toString)
      response.lastSyncedBlockTimestamp.value must be(
        dummySyncTimestamp.toProtoTimestamp
      )
    }
  }

  "NodeService.scheduleOperations" should {
    "fail when called with an empty sequence of operations" in {
      val error = intercept[StatusRuntimeException] {
        service.scheduleOperations(
          node_api
            .ScheduleOperationsRequest()
            .withSignedOperations(Seq())
        )
      }

      val expectedMessage =
        "INVALID_ARGUMENT: requirement failed: there must be at least one operation to be published"
      error.getMessage must be(expectedMessage)
    }

    "return error when at least one provided operation is invalid" in {
      val validOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val invalidOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation
          .update(
            _.createDid.didData.context := Seq("abc")
          ),
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )

      val error = intercept[StatusRuntimeException] {
        service.scheduleOperations(
          node_api
            .ScheduleOperationsRequest()
            .withSignedOperations(Seq(validOperation, invalidOperation))
        )
      }
      error.getStatus.getCode mustEqual Status.Code.INVALID_ARGUMENT
      verifyNoMoreInteractions(objectManagementService)
    }

    "properly return the result of a CreateDID operation and a DID Update operation" in {
      val createDIDOperation = BlockProcessingServiceSpec.signOperation(
        CreateDIDOperationSpec.exampleOperation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
      val createDIDOperationId = AtalaOperationId.of(createDIDOperation)

      val updateOperation = BlockProcessingServiceSpec.signOperation(
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation,
        "master",
        UpdateDIDOperationSpec.masterKeys.privateKey
      )
      val updateOperationId = AtalaOperationId.of(updateOperation)

      doReturn(
        fake[List[Either[NodeError, AtalaOperationId]]](
          List(Right(createDIDOperationId), Right(updateOperationId))
        )
      ).when(objectManagementService)
        .scheduleAtalaOperations(*)

      val response = service.scheduleOperations(
        node_api
          .ScheduleOperationsRequest()
          .withSignedOperations(Seq(createDIDOperation, updateOperation))
      )

      val expectedDIDSuffix =
        Sha256Hash
          .compute(CreateDIDOperationSpec.exampleOperation.toByteArray)
          .hexEncoded

      response.outputs.size mustBe (2)
      response.outputs.head.getCreateDidOutput.didSuffix mustBe expectedDIDSuffix
      response.outputs.head.operationMaybe.operationId.value mustEqual createDIDOperationId.toProtoByteString
      response.outputs.head.operationMaybe.error mustBe None

      response.outputs.last.result mustBe OperationOutput.Result
        .UpdateDidOutput(node_api.UpdateDIDOutput())
      response.outputs.last.operationMaybe.operationId.value mustEqual updateOperationId.toProtoByteString
      response.outputs.last.operationMaybe.error mustBe None

      verify(objectManagementService).scheduleAtalaOperations(
        createDIDOperation,
        updateOperation
      )
      verifyNoMoreInteractions(objectManagementService)
    }
  }
}
