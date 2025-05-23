package io.iohk.atala.prism.node.services

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.DataPreparation._
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.errors.NodeError.{
  TooManyDidPublicKeysCreationAttempt,
  TooManyServiceCreationAttempt,
  UnsupportedProtocolVersion
}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.{ApplyOperationConfig, CreateDIDOperationSpec}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec.{issuingEcKeyData, masterKeys}
import io.iohk.atala.prism.node.operations.ProtocolVersionUpdateOperationSpec._
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository,
  ProtocolVersionRepository
}
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec.{createDidOperation, signOperation}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.{DataPreparation, PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.node.utils.IOUtils._
import org.mockito
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import tofu.logging.Logs

import java.time.{Duration, Instant}

object ObjectManagementServiceSpec {
  private val newKeysPairs = List.fill(10) { CryptoTestUtils.generateKeyPair() }

  val exampleOperations: Seq[node_models.AtalaOperation] =
    newKeysPairs.zipWithIndex.map { case (keyPair: SecpPair, i) =>
      BlockProcessingServiceSpec.createDidOperation.update(
        _.createDid.didData.publicKeys.modify { keys =>
          keys :+ node_models.PublicKey(
            id = s"key$i",
            usage = node_models.KeyUsage.AUTHENTICATION_KEY,
            keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(
              CreateDIDOperationSpec.protoCompressedECKeyDataFromPublicKey(
                keyPair.publicKey
              )
            )
          )
        }
      )
    }

  val exampleSignedOperations: Seq[node_models.SignedAtalaOperation] =
    exampleOperations.map { operation =>
      BlockProcessingServiceSpec.signOperation(
        operation,
        "master",
        CreateDIDOperationSpec.masterKeys.privateKey
      )
    }
}

class ObjectManagementServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val ledger: UnderlyingLedger[IOWithTraceIdContext] =
    mock[UnderlyingLedger[IOWithTraceIdContext]]
  private val atalaOperationsRepository: AtalaOperationsRepository[IOWithTraceIdContext] =
    AtalaOperationsRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IOWithTraceIdContext] =
    AtalaObjectsTransactionsRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext] =
    KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, logs)
  private val blockProcessing: BlockProcessingService =
    mock[BlockProcessingService]
  private val protocolVersionRepository: ProtocolVersionRepository[IOWithTraceIdContext] =
    ProtocolVersionRepository.unsafe(
      dbLiftedToTraceIdIO,
      logs
    )
  private val publicKeysLimit = 4
  private val servicesLimit = 4

  private implicit lazy val submissionService: SubmissionService[IOWithTraceIdContext] =
    SubmissionService.unsafe(
      ledger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      logs = logs
    )

  private implicit lazy val objectManagementService: ObjectManagementService[IOWithTraceIdContext] =
    ObjectManagementService.unsafe(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionRepository,
      blockProcessing,
      publicKeysLimit,
      servicesLimit,
      dbLiftedToTraceIdIO,
      logs
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "ObjectManagementService.scheduleAtalaOperations" should {
    "update status to received when operation was received, but haven't processed yet" in {
      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(dummyPublicationInfo)
          )
      ).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId =
        BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedOperationId =
        publishSingleOperationAndFlush(atalaOperation).futureValue.toOption.get
      returnedOperationId mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService
        .getOperationInfo(atalaOperationId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(
        Some(AtalaObjectTransactionSubmissionStatus.Pending)
      )
      atalaOperationInfo.transactionId.value must be(
        dummyPublicationInfo.transaction.transactionId
      )
    }

    "ignore publishing duplicate operation" in {
      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(dummyPublicationInfo)
          )
      ).when(ledger).publish(*)

      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, TransactionDetails]](
            Right(TransactionDetails(dummyPublicationInfo.transaction.transactionId, TransactionStatus.Pending))
          )
      ).when(ledger).getTransactionDetails(dummyPublicationInfo.transaction.transactionId)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId =
        BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedAtalaOperation =
        publishSingleOperationAndFlush(atalaOperation).futureValue
      returnedAtalaOperation must be(Right(atalaOperationId))

      val operationId =
        publishSingleOperationAndFlush(atalaOperation).futureValue
      operationId must be(Right(atalaOperationId))

      val atalaOperationInfo = objectManagementService
        .getOperationInfo(atalaOperationId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(
        Some(AtalaObjectTransactionSubmissionStatus.Pending)
      )
      atalaOperationInfo.transactionId.value must be(
        dummyPublicationInfo.transaction.transactionId
      )
    }

    "ignore publishing duplicate operation in the same block" in {
      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(dummyPublicationInfo)
          )
      ).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId =
        BlockProcessingServiceSpec.signedCreateDidOperationId

      val opIds =
        publishOperationsAndFlush(atalaOperation, atalaOperation).futureValue

      opIds.size mustBe 2
      opIds.head mustBe Right(atalaOperationId)
      opIds.last mustBe Right(atalaOperationId)

      val atalaOperationInfo = objectManagementService
        .getOperationInfo(atalaOperationId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(
        Some(AtalaObjectTransactionSubmissionStatus.Pending)
      )
    }

    "put block content onto the ledger when supported" in {
      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(dummyPublicationInfo)
          )
      ).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(
          BlockProcessingServiceSpec.signedCreateDidOperation
        ).futureValue.toOption.get

      returnedOperationId mustBe BlockProcessingServiceSpec.signedCreateDidOperationId
      // Verify published AtalaObject
      val atalaObjectCaptor = ArgCaptor[node_models.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)
      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = atalaObject.blockContent.value
      atalaBlock.operations must contain theSameElementsAs Seq(
        BlockProcessingServiceSpec.signedCreateDidOperation
      )
      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(
        AtalaObjectTransactionSubmissionStatus.Pending
      )
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService
        .getOperationInfo(returnedOperationId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(
        AtalaObjectTransactionSubmissionStatus.Pending
      )
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(
        dummyPublicationInfo.transaction.transactionId
      )
    }

    "record immediate in-ledger transactions" in {
      val inLedgerPublication =
        dummyPublicationInfo.copy(status = TransactionStatus.InLedger)
      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(inLedgerPublication)
          )
      ).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(
          BlockProcessingServiceSpec.signedCreateDidOperation
        ).futureValue.toOption.get

      returnedOperationId must be(
        BlockProcessingServiceSpec.signedCreateDidOperationId
      )

      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(
        AtalaObjectTransactionSubmissionStatus.InLedger
      )
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService
        .getOperationInfo(returnedOperationId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(
        AtalaObjectTransactionSubmissionStatus.InLedger
      )
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(
        inLedgerPublication.transaction.transactionId
      )
    }

    "ignore publishing as node doesn't support current protocol version" in {
      increaseCurrentProtocolVersion()

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val currentVersion = ProtocolVersion(2, 0)
      val expectedException =
        UnsupportedProtocolVersion(currentVersion).toStatus.asRuntimeException

      ScalaFutures.whenReady(
        publishSingleOperationAndFlush(atalaOperation).failed
      ) { err =>
        err.toString mustBe expectedException.toString
      }
    }

    "fail scheduling when CreateDID contains too much public keys" in {
      val createOperation = createDidOperation.update(
        _.createDid.didData.publicKeys :+= node_models.PublicKey(
          "issuing1",
          node_models.KeyUsage.ISSUING_KEY,
          Some(
            node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
          ),
          None,
          node_models.PublicKey.KeyData.CompressedEcKeyData(issuingEcKeyData)
        )
      )
      val signedCreateDidOperation =
        signOperation(createOperation, "master", masterKeys.privateKey)

      val result =
        publishSingleOperationAndFlush(signedCreateDidOperation).futureValue

      result.left.value must be(TooManyDidPublicKeysCreationAttempt(4, 5))
    }

    "fail scheduling when createDID contains too much services" in {
      val createOperation = createDidOperation.update(
        _.createDid.didData.services :++= Array
          .tabulate(5) { index =>
            node_models.Service(
              id = "linked-domain" + index,
              `type` = "didCom-credential-exchange",
              serviceEndpoint = "https://baz.example.com/",
              addedOn = None,
              deletedOn = None
            )
          }
          .toList
      )
      val signedCreateDidOperation =
        signOperation(createOperation, "master", masterKeys.privateKey)

      val result =
        publishSingleOperationAndFlush(signedCreateDidOperation).futureValue

      result.left.value must be(TooManyServiceCreationAttempt(4, 7))
    }

  }

  // needed because mockito doesn't interact too nicely with value classes
  private def anyTransactionIdMatcher =
    mockito.ArgumentMatchers.any[Array[Byte]].asInstanceOf[TransactionId]

  "ObjectManagementService.saveObject" should {
    "add object to the database when nonexistent (unpublished)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "update object to the database when existing without transaction info (published but not confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      doReturn(
        ReaderT
          .pure[IO, TraceId, Either[CardanoWalletError, PublicationInfo]](
            Right(
              PublicationInfo(dummyTransactionInfo, TransactionStatus.InLedger)
            )
          )
      ).when(ledger)
        .publish(*)

      val signedOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val obj = createAtalaObject(createBlock(signedOperation))
      val operationId =
        publishSingleOperationAndFlush(signedOperation).futureValue.toOption.get

      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      val atalaObject = queryAtalaObject(obj)
      val operationInfo =
        objectManagementService
          .getOperationInfo(operationId)
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .futureValue

      atalaObject.transaction.value mustBe dummyTransactionInfo
      // We don't check the whole returned AtalaOperationInfo because `operationStatus`
      // has to be APPLIED but it's RECEIVED due to blockService mock doesn't perform any real actions and db updates
      operationInfo.value.transactionSubmissionStatus mustBe Some(InLedger)
      operationInfo.value.transactionId mustBe Some(
        dummyTransactionInfo.transactionId
      )
    }

    "not update the object when existing with transaction info (confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
      val dummyTransactionInfo2 = TransactionInfo(
        transactionId = TransactionId.from(Sha256Hash.compute("id".getBytes).bytes).value,
        ledger = Ledger.InMemory,
        block = Some(BlockInfo(number = 100, timestamp = Instant.now, index = 100))
      )
      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo2))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "process the block" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      val block = createBlock()
      val obj = createAtalaObject(block)
      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      val blockCaptor = ArgCaptor[node_models.AtalaBlock]
      verify(blockProcessing).processBlock(
        blockCaptor,
        // mockito hates value classes, so we cannot test equality to this argument
        anyTransactionIdMatcher,
        mockito.ArgumentMatchers.eq(dummyTransactionInfo.ledger),
        mockito.ArgumentMatchers.eq(
          Instant.ofEpochMilli(dummyTimestamp.toEpochMilli)
        ),
        mockito.ArgumentMatchers.eq(dummyABSequenceNumber)
      )
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = queryAtalaObject(obj)
      atalaObject.status mustBe AtalaObjectStatus.Processed
    }

    "ignore block when current protocol version isn't supported by node" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)

      increaseCurrentProtocolVersion()

      val block = createBlock()
      val obj = createAtalaObject(block)
      objectManagementService
        .saveObject(AtalaObjectNotification(obj, dummyTransactionInfo))
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue

      val atalaObject = AtalaObjectsDAO
        .get(AtalaObjectId.of(obj))
        .transact(database)
        .unsafeRunSync()
      atalaObject mustBe None
    }

    def queryAtalaObject(obj: node_models.AtalaObject): AtalaObjectInfo = {
      AtalaObjectsDAO
        .get(AtalaObjectId.of(obj))
        .transact(database)
        .unsafeRunSync()
        .value
    }
  }

  private def queryTransactionSubmissions(
      status: AtalaObjectTransactionSubmissionStatus
  ): List[AtalaObjectTransactionSubmission] = {
    // Query into the future to return all of them
    AtalaObjectTransactionSubmissionsDAO
      .getBy(Instant.now.plus(Duration.ofSeconds(1)), status, ledger.getType)
      .transact(database)
      .unsafeRunSync()
  }

  def increaseCurrentProtocolVersion() = {
    DataPreparation
      .createDID(
        DIDData(
          proposerDIDSuffix,
          proposerDidKeys,
          Nil,
          Nil,
          proposerCreateDIDOperation.digest
        ),
        dummyLedgerData
      )

    val protUpd1 = parsedProtocolUpdateOperation1
      .applyState(ApplyOperationConfig(proposerDIDSuffix))
      .transact(database)
      .value
      .unsafeToFuture()
      .futureValue
    protUpd1 mustBe Right(())

    val effectiveVers = protocolVersionRepository
      .markEffective(10)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .futureValue
    effectiveVers mustBe Some(protocolVersionInfo1)
  }
}
