package io.iohk.atala.prism.node.services

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models._
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClientImpl
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.dbsync.repositories.testing.TestCardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata.METADATA_PRISM_INDEX
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.testing.FakeCardanoWalletApiClient
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.services.CardanoLedgerService.{CardanoBlockHandler, CardanoNetwork}
import io.iohk.atala.prism.node.services.models.testing.TestAtalaHandlers
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.utils.BytesOps
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._
import tofu.logging.Logs

class CardanoLedgerServiceSpec extends AtalaWithPostgresSpec {
  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val network = CardanoNetwork.Testnet
  private val ledger = Ledger.CardanoTestnet
  private val walletId: WalletId =
    WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value
  private val walletPassphrase = "Secure Passphrase"
  private val paymentAddress: Address = Address(
    "2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb"
  )
  private val blockConfirmationsToWait = 31

  private val noOpObjectHandler: AtalaObjectNotificationHandler[IOWithTraceIdContext] = _ => ReaderT.pure(())
  private val noOpBlockHandler: CardanoBlockHandler[IOWithTraceIdContext] = _ => ReaderT.pure(())
  private lazy val keyValueService = KeyValueService.unsafe(
    KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, logs),
    logs
  )
  private lazy val cardanoBlockRepository =
    CardanoBlockRepository.unsafe(dbLiftedToTraceIdIO, logs)

  override def beforeAll(): Unit = {
    super.beforeAll()

    TestCardanoBlockRepository.createTables()
  }

  "publish" should {
    val atalaObject = node_internal
      .AtalaObject()
      .withBlockContent(node_internal.AtalaBlock())
    val expectedWalletApiPath = s"v2/wallets/$walletId/transactions"

    "publish an object" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Success[IOWithTraceIdContext](
          expectedWalletApiPath,
          readResource("publishReference_cardanoWalletApiRequest.json"),
          readResource("publishReference_success_cardanoWalletApiResponse.json")
        )
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      // Only test that it doesn't fail, as calling the wrong endpoint with the wrong params fails
      cardanoLedgerService
        .publish(atalaObject)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
    }

    "fail with the CannotCoverFee when no money for publishing the transaction" in {
      val errorCode = "cannot_cover_fee"
      val errorMessage =
        "May occur when a transaction can't be balanced for fees."
      val exceptionDescription = f"Status [$errorCode]. $errorMessage"

      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Fail[IOWithTraceIdContext](
          expectedWalletApiPath,
          readResource("publishReference_cardanoWalletApiRequest.json"),
          errorCode,
          errorMessage
        )
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      val err = cardanoLedgerService
        .publish(atalaObject)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .left
        .toOption
        .value
      err.getMessage must be(exceptionDescription)
    }
  }

  "getTransactionDetails" should {
    val transactionId = TransactionId
      .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
      .value
    val expectedWalletApiPath =
      s"v2/wallets/$walletId/transactions/$transactionId"

    "get the transaction details" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Success[IOWithTraceIdContext](
          expectedWalletApiPath,
          "",
          readResource("getTransaction_success_cardanoWalletApiResponse.json")
        )
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      val transactionDetails = cardanoLedgerService
        .getTransactionDetails(transactionId)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      transactionDetails must be(
        TransactionDetails(transactionId, TransactionStatus.InLedger)
      )
    }

    "fail to get the transaction details when the wallet fails" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Fail[IOWithTraceIdContext](
          expectedWalletApiPath,
          "",
          "internal",
          "Internal error"
        )
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      val err = cardanoLedgerService
        .getTransactionDetails(transactionId)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .left
        .toOption
        .value
      err.code must be(CardanoWalletErrorCode.UndefinedCardanoWalletError)
    }
  }

  "deleteTransaction" should {
    val transactionId = TransactionId
      .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
      .value
    val expectedWalletApiPath =
      s"v2/wallets/$walletId/transactions/$transactionId"

    "delete a transaction" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient
          .Success[IOWithTraceIdContext](expectedWalletApiPath, "", "")
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      cardanoLedgerService
        .deleteTransaction(transactionId)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
    }

    "fail to delete a transaction when the wallet fails" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Fail[IOWithTraceIdContext](
          expectedWalletApiPath,
          "",
          "transaction_already_in_ledger",
          "Occurs when attempting to delete a transaction which is neither pending nor expired."
        )
      val cardanoLedgerService =
        createCardanoLedgerService(cardanoWalletApiClient)

      val error = cardanoLedgerService
        .deleteTransaction(transactionId)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .left
        .toOption
        .value

      val expectedErrorMessage =
        "Status [transaction_already_in_ledger]. Occurs when attempting to delete a transaction which is neither pending nor expired."
      error.getMessage must be(expectedErrorMessage)
    }
  }

  "syncAtalaObjects" should {

    /** Creates `totalBlockCount` blocks and appends one transaction with PRISM metadata to every given
      * `blocksWithNotifications`, returning all expected notifications.
      */
    def createNotificationsInDb(
        totalBlockCount: Int,
        blocksWithNotifications: Int*
    ): Seq[AtalaObjectNotification] = {
      val allBlocks =
        TestCardanoBlockRepository.createRandomBlocks(totalBlockCount)
      allBlocks.foreach(TestCardanoBlockRepository.insertBlock)

      blocksWithNotifications.map { blockWithNotification =>
        val block = allBlocks(blockWithNotification)
        val atalaObject = node_internal
          .AtalaObject()
          .withBlockContent(
            node_internal.AtalaBlock(operations = Seq())
          )
        val blockIndex = block.transactions.size
        val transaction = Transaction(
          TestCardanoBlockRepository.randomTransactionId(),
          block.header.hash,
          blockIndex,
          Some(toTransactionMetadata(atalaObject))
        )
        TestCardanoBlockRepository.insertTransaction(transaction, blockIndex)

        AtalaObjectNotification(
          atalaObject,
          TransactionInfo(
            transactionId = transaction.id,
            ledger = ledger,
            block = Some(
              BlockInfo(
                number = blockWithNotification,
                timestamp = block.header.time,
                index = blockIndex
              )
            )
          )
        )
      }
    }

    // AtalaObjectMetadata.toTransactionMetadata cannot be used as the format received by cardano-db-sync is not
    // compatible
    def toTransactionMetadata(
        atalaObject: node_internal.AtalaObject
    ): TransactionMetadata = {
      TransactionMetadata(
        Json.obj(
          METADATA_PRISM_INDEX.toString -> Json.obj(
            "v" -> Json.fromInt(1),
            "c" -> Json.arr(
              atalaObject.toByteArray
                .grouped(64)
                .map(bytes => Json.fromString(BytesOps.bytesToHex(bytes)))
                .toSeq: _*
            )
          )
        )
      )
    }

    "sync Atala objects in confirmed blocks" in {
      val totalBlockCount = 50
      // Append a transaction with PRISM metadata to the last confirmed block
      val allNotifications = createNotificationsInDb(
        totalBlockCount,
        totalBlockCount - blockConfirmationsToWait
      )
      // Configure the service to capture received notifications
      val notificationHandler = new TestAtalaHandlers[IOWithTraceIdContext]()
      val cardanoLedgerService =
        createCardanoLedgerService(
          FakeCardanoWalletApiClient.NotFound(),
          onAtalaObject = notificationHandler.asAtalaObjectHandler
        )

      val pendingBlocks = cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      pendingBlocks must be(false)
      notificationHandler.receivedNotifications must be(allNotifications)
    }

    "sync not more than 100 Atala objects" in {
      val totalBlockCount = 200
      val allNotifications = createNotificationsInDb(totalBlockCount, 100, 101)
      // Configure the service to capture received notifications
      val notificationHandler = new TestAtalaHandlers[IOWithTraceIdContext]()
      val cardanoLedgerService =
        createCardanoLedgerService(
          FakeCardanoWalletApiClient.NotFound(),
          onAtalaObject = notificationHandler.asAtalaObjectHandler
        )

      val pendingBlocks = cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      pendingBlocks must be(true)
      // Only notification for block #100 should be received, as block #101 is not yet synced
      notificationHandler.receivedNotifications must be(
        allNotifications.take(1)
      )
    }

    "start syncing at specified block" in {
      val totalBlockCount = 50
      val blockNumberSyncStart = 10
      // Append an Atala transaction to the start sync block and the previous one
      val allNotifications = createNotificationsInDb(
        totalBlockCount,
        blockNumberSyncStart - 1,
        blockNumberSyncStart
      )
      // Configure the service to capture received notifications
      val notificationHandler = new TestAtalaHandlers[IOWithTraceIdContext]()
      val cardanoLedgerService =
        createCardanoLedgerService(
          FakeCardanoWalletApiClient.NotFound(),
          blockNumberSyncStart = blockNumberSyncStart,
          onAtalaObject = notificationHandler.asAtalaObjectHandler
        )

      val pendingBlocks = cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      pendingBlocks must be(false)
      // Block #9 should be skipped over
      notificationHandler.receivedNotifications must be(
        List(allNotifications(1))
      )
    }

    "not sync Atala objects in unconfirmed blocks" in {
      val totalBlockCount = 50
      // Append a transaction with PRISM metadata to the first unconfirmed block
      createNotificationsInDb(
        totalBlockCount,
        totalBlockCount - blockConfirmationsToWait + 1
      )
      // Configure the service to capture received notifications
      val notificationHandler = new TestAtalaHandlers[IOWithTraceIdContext]()
      val cardanoLedgerService =
        createCardanoLedgerService(
          FakeCardanoWalletApiClient.NotFound(),
          onAtalaObject = notificationHandler.asAtalaObjectHandler
        )

      val pendingBlocks = cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      pendingBlocks must be(false)
      notificationHandler.receivedNotifications must be(List())
    }

    "only sync Atala objects when their block become confirmed" in {
      val totalBlockCount = 50
      // Append a transaction with PRISM metadata to the last confirmed and the first unconfirmed block
      val notifications = createNotificationsInDb(
        totalBlockCount,
        totalBlockCount - blockConfirmationsToWait,
        totalBlockCount - blockConfirmationsToWait + 1
      )
      // Configure the service to capture received notifications
      val notificationHandler = new TestAtalaHandlers[IOWithTraceIdContext]()
      val cardanoLedgerService =
        createCardanoLedgerService(
          FakeCardanoWalletApiClient.NotFound[IOWithTraceIdContext](),
          onAtalaObject = notificationHandler.asAtalaObjectHandler
        )

      // Test #1: only the first object is synced
      cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      notificationHandler.receivedNotifications must be(notifications.take(1))

      // Append a new block
      val lastBlock =
        cardanoBlockRepository
          .getFullBlock(totalBlockCount)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
      TestCardanoBlockRepository.insertBlock(
        TestCardanoBlockRepository.createNextRandomBlock(Some(lastBlock))
      )

      // Test #2: all objects are now synced
      cardanoLedgerService
        .syncAtalaObjects()
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      notificationHandler.receivedNotifications must be(notifications)
    }
  }

  private def createCardanoLedgerService(
      cardanoWalletApiClient: CardanoWalletApiClient[IOWithTraceIdContext],
      blockNumberSyncStart: Int = 0,
      onAtalaObject: AtalaObjectNotificationHandler[IOWithTraceIdContext] = noOpObjectHandler
  ): CardanoLedgerService[IOWithTraceIdContext] = {
    val cardanoClient = createCardanoClient(cardanoWalletApiClient)
    new CardanoLedgerService(
      network,
      walletId,
      walletPassphrase,
      paymentAddress,
      blockNumberSyncStart,
      blockConfirmationsToWait,
      cardanoClient,
      keyValueService,
      noOpBlockHandler,
      onAtalaObject
    )
  }

  private def createCardanoClient(
      cardanoWalletApiClient: CardanoWalletApiClient[IOWithTraceIdContext]
  ): CardanoClient[IOWithTraceIdContext] = {
    CardanoClient.makeUnsafe(
      new CardanoDbSyncClientImpl(cardanoBlockRepository),
      cardanoWalletApiClient
    )
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"services/cardano/$resource").mkString
    } catch {
      case _: Throwable =>
        throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
