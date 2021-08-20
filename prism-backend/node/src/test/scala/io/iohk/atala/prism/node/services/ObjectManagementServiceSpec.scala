package io.iohk.atala.prism.node.services

import cats.effect.IO
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository
}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.DataPreparation._
import io.iohk.atala.prism.protos.{node_internal, node_models}
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.mockito
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{MockitoSugar, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures

import java.time.{Duration, Instant}
import scala.concurrent.Future

object ObjectManagementServiceSpec {
  private val newKeysPairs = List.fill(10) { EC.generateKeyPair() }

  val exampleOperations: Seq[node_models.AtalaOperation] = newKeysPairs.zipWithIndex.map {
    case (keyPair: ECKeyPair, i) =>
      BlockProcessingServiceSpec.createDidOperation.update(_.createDid.didData.publicKeys.modify { keys =>
        keys :+ node_models.PublicKey(
          id = s"key$i",
          usage = node_models.KeyUsage.AUTHENTICATION_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            CreateDIDOperationSpec.protoECKeyFromPublicKey(keyPair.getPublicKey)
          )
        )
      })
  }

  val exampleSignedOperations: Seq[node_models.SignedAtalaOperation] = exampleOperations.map { operation =>
    BlockProcessingServiceSpec.signOperation(operation, "master", CreateDIDOperationSpec.masterKeys.getPrivateKey)
  }
}

class ObjectManagementServiceSpec
    extends AtalaWithPostgresSpec
    with MockitoSugar
    with ResetMocksAfterEachTest
    with BeforeAndAfterEach {
  private val ledger: UnderlyingLedger = mock[UnderlyingLedger]
  private val atalaOperationsRepository: AtalaOperationsRepository[IO] = AtalaOperationsRepository(database)
  private val atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IO] =
    AtalaObjectsTransactionsRepository(database)
  private val keyValuesRepository: KeyValuesRepository[IO] = KeyValuesRepository(database)
  private val blockProcessing: BlockProcessingService = mock[BlockProcessingService]

  private implicit lazy val submissionService: SubmissionService =
    SubmissionService(
      ledger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository
    )

  private implicit lazy val objectManagementService: ObjectManagementService =
    ObjectManagementService(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      blockProcessing
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    doReturn(Ledger.InMemory).when(ledger).getType
    ()
  }

  "ObjectManagementService.publishAtalaOperation" should {
    "update status to received when operation was received, but haven't processed yet" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedOperationId = publishSingleOperationAndFlush(atalaOperation).futureValue
      returnedOperationId mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId
      val returnedAtalaOperation = publishSingleOperationAndFlush(atalaOperation).futureValue
      returnedAtalaOperation mustBe atalaOperationId

      ScalaFutures.whenReady(publishSingleOperationAndFlush(atalaOperation).failed) { err =>
        err mustBe a[DuplicateAtalaBlock]
      }

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
      atalaOperationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "ignore publishing duplicate operation in the same block" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val atalaOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val atalaOperationId = BlockProcessingServiceSpec.signedCreateDidOperationId

      val opIds = publishOperationsAndFlush(atalaOperation, atalaOperation).futureValue

      opIds.size mustBe 2
      opIds.head mustBe atalaOperationId
      opIds.last mustBe atalaOperationId

      val atalaOperationInfo = objectManagementService.getOperationInfo(atalaOperationId).futureValue.value

      atalaOperationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      atalaOperationInfo.transactionSubmissionStatus must be(Some(AtalaObjectTransactionSubmissionStatus.Pending))
    }

    "put block content onto the ledger when supported" in {
      doReturn(Future.successful(Right(dummyPublicationInfo))).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      returnedOperationId mustBe BlockProcessingServiceSpec.signedCreateDidOperationId
      // Verify published AtalaObject
      val atalaObjectCaptor = ArgCaptor[node_internal.AtalaObject]
      verify(ledger).publish(atalaObjectCaptor)
      val atalaObject = atalaObjectCaptor.value
      val atalaBlock = atalaObject.blockContent.value
      atalaBlock.operations must contain theSameElementsAs Seq(BlockProcessingServiceSpec.signedCreateDidOperation)
      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.Pending)
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService.getOperationInfo(returnedOperationId).futureValue.value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(AtalaObjectTransactionSubmissionStatus.Pending)
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(dummyPublicationInfo.transaction.transactionId)
    }

    "record immediate in-ledger transactions" in {
      val inLedgerPublication = dummyPublicationInfo.copy(status = TransactionStatus.InLedger)
      doReturn(Future.successful(Right(inLedgerPublication))).when(ledger).publish(*)

      val returnedOperationId =
        publishSingleOperationAndFlush(BlockProcessingServiceSpec.signedCreateDidOperation).futureValue

      returnedOperationId must be(BlockProcessingServiceSpec.signedCreateDidOperationId)

      // Verify transaction submission
      val transactionSubmissions = queryTransactionSubmissions(AtalaObjectTransactionSubmissionStatus.InLedger)
      transactionSubmissions.size mustBe 1

      val operationInfo = objectManagementService.getOperationInfo(returnedOperationId).futureValue.value
      operationInfo.operationId must be(returnedOperationId)
      operationInfo.transactionSubmissionStatus.value must be(AtalaObjectTransactionSubmissionStatus.InLedger)
      operationInfo.operationStatus must be(AtalaOperationStatus.RECEIVED)
      operationInfo.transactionId.value must be(inLedgerPublication.transaction.transactionId)
    }
  }

  // needed because mockito doesn't interact too nicely with value classes
  private def anyTransactionIdMatcher = mockito.ArgumentMatchers.any[Array[Byte]].asInstanceOf[TransactionId]

  "ObjectManagementService.saveObject" should {
    "add object to the database when nonexistent (unpublished)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "update object to the database when existing without transaction info (published but not confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val signedOperation = BlockProcessingServiceSpec.signedCreateDidOperation
      val obj = createAtalaObject(createBlock(signedOperation))
      publishSingleOperationAndFlush(signedOperation)

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val atalaObject = queryAtalaObject(obj)
      atalaObject.transaction.value mustBe dummyTransactionInfo
      // TODO: Once processing is async, test the object is not processed
    }

    "not update the object when existing with transaction info (confirmed)" in {
      doReturn(connection.pure(true))
        .when(blockProcessing)
        .processBlock(*, anyTransactionIdMatcher, *, *, *)
      val obj = createAtalaObject()

      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue
      val dummyTransactionInfo2 = TransactionInfo(
        transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).getValue).value,
        ledger = Ledger.InMemory,
        block = Some(BlockInfo(number = 100, timestamp = Instant.now, index = 100))
      )
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo2)).futureValue

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
      objectManagementService.saveObject(AtalaObjectNotification(obj, dummyTransactionInfo)).futureValue

      val blockCaptor = ArgCaptor[node_internal.AtalaBlock]
      verify(blockProcessing).processBlock(
        blockCaptor,
        // mockito hates value classes, so we cannot test equality to this argument
        anyTransactionIdMatcher,
        mockito.ArgumentMatchers.eq(dummyTransactionInfo.ledger),
        mockito.ArgumentMatchers.eq(Instant.ofEpochMilli(dummyTimestamp.toEpochMilli)),
        mockito.ArgumentMatchers.eq(dummyABSequenceNumber)
      )
      blockCaptor.value mustEqual block

      verifyNoMoreInteractions(blockProcessing)

      val atalaObject = queryAtalaObject(obj)
      atalaObject.processed mustBe true
    }

    def queryAtalaObject(obj: node_internal.AtalaObject): AtalaObjectInfo = {
      AtalaObjectsDAO.get(AtalaObjectId.of(obj)).transact(database).unsafeRunSync().value
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
}
