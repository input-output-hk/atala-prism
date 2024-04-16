package io.iohk.atala.prism.node

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.stub.MetadataUtils
import io.grpc.{ManagedChannel, Server, StatusRuntimeException}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.auth.WhitelistedAuthenticatorF
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticatorInterceptor
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, Lovelace}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.AtalaObjectStatus.{Pending, Scheduled}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.{
  AtalaOperationsRepository,
  MetricsCountersRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.node.services.{
  BlockProcessingServiceSpec,
  NodeExplorerService,
  ObjectManagementService,
  StatisticsService
}
import io.iohk.atala.prism.nonce.{ClientHelper, RequestAuthenticator}
import io.iohk.atala.prism.protos.node_api.GetScheduledOperationsRequest.OperationType.{
  AnyOperationType,
  CreateDidOperationOperationType,
  IssueCredentialBatchOperationType
}
import io.iohk.atala.prism.protos.node_api.NodeExplorerServiceGrpc.NodeExplorerServiceBlockingClient
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_api, node_internal, node_models}
import io.iohk.atala.prism.utils.IOUtils.ioComonad
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import tofu.logging.Logs

class NodeExplorerServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: node_api.NodeExplorerServiceGrpc.NodeExplorerServiceBlockingStub = _

  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val underlyingLedger =
    mock[UnderlyingLedger[IOWithTraceIdContext]]
  private val objectManagementService =
    mock[ObjectManagementService[IOWithTraceIdContext]]

  private val masterKeyPair = EC.generateKeyPair()
  private val whitelistedDid = DID.buildLongFormFromMasterPublicKey(masterKeyPair.getPublicKey)

  private val metricsCountersRepository = mock[MetricsCountersRepository[IOWithTraceIdContext]]

  private val atalaOperationsRepository = mock[AtalaOperationsRepository[IOWithTraceIdContext]]

  private val requestNoncesRepo = RequestNoncesRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val authenticator = WhitelistedAuthenticatorF.unsafe(new NodeExplorerAuthenticator(requestNoncesRepo), logs)

  def fake[T](a: T): ReaderT[IO, TraceId, T] =
    ReaderT.apply[IO, TraceId, T](_ => IO.pure(a))

  override def beforeEach(): Unit = {
    super.beforeEach()

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .intercept(new GrpcAuthenticatorInterceptor)
      .directExecutor()
      .addService(
        node_api.NodeExplorerServiceGrpc
          .bindService(
            new NodeExplorerGrpcServiceImpl(
              authenticator,
              NodeExplorerService.unsafe(underlyingLedger, objectManagementService, logs),
              StatisticsService.unsafe(atalaOperationsRepository, metricsCountersRepository, logs),
              Set(whitelistedDid)
            ),
            executionContext
          )
      )
      .build()
      .start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()

    service = node_api.NodeExplorerServiceGrpc.blockingStub(channelHandle)
  }

  private def withNonce(
      s: node_api.NodeExplorerServiceGrpc.NodeExplorerServiceBlockingStub
  ): NodeExplorerServiceBlockingClient = {

    val requestAuthenticator = new RequestAuthenticator
    val requestSigner = ClientHelper.requestSigner(
      requestAuthenticator,
      whitelistedDid,
      masterKeyPair.getPrivateKey
    )

    def addAuthHeader(
        request: scalapb.GeneratedMessage
    ): node_api.NodeExplorerServiceGrpc.NodeExplorerServiceBlockingStub = {
      val header = requestSigner(request)
      s.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(header.toMetadata)
      )
    }

    new NodeExplorerServiceBlockingClient() {
      override def getScheduledOperations(request: GetScheduledOperationsRequest): GetScheduledOperationsResponse =
        addAuthHeader(request).getScheduledOperations(request)

      override def getWalletTransactionsPaginated(
          request: GetWalletTransactionsRequest
      ): GetWalletTransactionsResponse =
        addAuthHeader(request).getWalletTransactionsPaginated(request)

      override def getWalletBalance(request: GetWalletBalanceRequest): GetWalletBalanceResponse =
        addAuthHeader(request).getWalletBalance(request)

      override def getAvailableMetrics(request: GetAvailableMetricsRequest): GetAvailableMetricsResponse =
        addAuthHeader(request).getAvailableMetrics(request)

      override def getNodeStatistics(request: GetNodeStatisticsRequest): GetNodeStatisticsResponse =
        addAuthHeader(request).getNodeStatistics(request)
    }

  }

  "NodeExplorerService.getWalletBalance" should {
    "return wallet balance" in {
      val walletBalance = Balance(Lovelace(2))

      doReturn(
        fake[Either[CardanoWalletError, Balance]](Right[CardanoWalletError, Balance](walletBalance))
      ).when(underlyingLedger).getWalletBalance

      val getWalletBalanceResponse = withNonce(service).getWalletBalance(GetWalletBalanceRequest())

      getWalletBalanceResponse.balance mustBe ByteString.copyFrom(walletBalance.available.toByteArray)
    }

    "thrown an unauthenticated error" in {
      val walletBalance = Balance(Lovelace(2))

      doReturn(
        fake[Either[CardanoWalletError, Balance]](Right[CardanoWalletError, Balance](walletBalance))
      ).when(underlyingLedger).getWalletBalance

      assertThrows[StatusRuntimeException] {
        service.getWalletBalance(GetWalletBalanceRequest())
      }
    }
  }

  "NodeExplorerService.getScheduledAtalaOperations" should {
    "return scheduled operations in correct order" in {
      def sign(op: node_models.AtalaOperation): SignedAtalaOperation =
        BlockProcessingServiceSpec.signOperation(op, "master", CreateDIDOperationSpec.masterKeys.privateKey)

      def toAtalaObject(ops: List[node_models.AtalaOperation]): node_internal.AtalaObject = {
        val block = node_internal.AtalaBlock(ops.map(sign))
        node_internal.AtalaObject(
          blockContent = Some(block)
        )
      }

      def toOperation(op: node_models.AtalaOperation) =
        operations
          .parseOperationWithMockedLedger(sign(op))
          .toOption
          .value

      val ops1 = List[node_models.AtalaOperation](
        CreateDIDOperationSpec.exampleOperation,
        UpdateDIDOperationSpec.exampleAddAndRemoveOperation
      )

      val ops2 = List[node_models.AtalaOperation](
        UpdateDIDOperationSpec.exampleRemoveOperation,
        IssueCredentialBatchOperationSpec.exampleOperation,
        CreateDIDOperationSpec.exampleOperationWithCompressedKeys
      )

      val allOps: List[node_models.AtalaOperation] =
        List(
          CreateDIDOperationSpec.exampleOperation,
          UpdateDIDOperationSpec.exampleAddAndRemoveOperation,
          UpdateDIDOperationSpec.exampleRemoveOperation,
          IssueCredentialBatchOperationSpec.exampleOperation,
          CreateDIDOperationSpec.exampleOperationWithCompressedKeys
        )
      val opsCreation: List[node_models.AtalaOperation] =
        List(CreateDIDOperationSpec.exampleOperation, CreateDIDOperationSpec.exampleOperationWithCompressedKeys)

      val opsIssuance: List[node_models.AtalaOperation] = List(IssueCredentialBatchOperationSpec.exampleOperation)

      val obj1 = toAtalaObject(ops1)
      val obj2 = toAtalaObject(ops2)

      val dummyObjects = List(
        AtalaObjectInfo(AtalaObjectId.of(obj1), obj1.toByteArray, ops1.map(toOperation), Pending, None),
        AtalaObjectInfo(AtalaObjectId.of(obj2), obj2.toByteArray, ops2.map(toOperation), Scheduled, None)
      )

      doReturn(fake[Either[NodeError, List[AtalaObjectInfo]]](Right(dummyObjects)))
        .when(objectManagementService)
        .getScheduledAtalaObjects

      val responseAny =
        withNonce(service).getScheduledOperations(GetScheduledOperationsRequest(AnyOperationType))
      val responseCreation =
        withNonce(service).getScheduledOperations(GetScheduledOperationsRequest(CreateDidOperationOperationType))
      val responseIssuance =
        withNonce(service).getScheduledOperations(GetScheduledOperationsRequest(IssueCredentialBatchOperationType))

      responseAny.scheduledOperations.map(_.operation.get) must be(allOps)
      responseCreation.scheduledOperations.map(_.operation.get) must be(opsCreation)
      responseIssuance.scheduledOperations.map(_.operation.get) must be(opsIssuance)
    }
  }
}
