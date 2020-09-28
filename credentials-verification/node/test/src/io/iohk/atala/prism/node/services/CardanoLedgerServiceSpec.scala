package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.dbsync.repositories.testing.TestCardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.testing.FakeCardanoWalletApiClient
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.node.services.models.AtalaObjectNotificationHandler
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.prism.protos.node_internal
import monix.execution.schedulers.TestScheduler
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

class CardanoLedgerServiceSpec extends PostgresRepositorySpec {
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"

  private val walletId: WalletId = WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value
  private val walletPassphrase = "Secure Passphrase"
  private val paymentAddress: Address = Address("2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb")
  private val blockConfirmationsToWait = 31

  private val noOpObjectHandler: AtalaObjectNotificationHandler = _ => Future.unit
  private val scheduler: TestScheduler = TestScheduler()
  private lazy val keyValueService = new KeyValueService(new KeyValuesRepository(database))

  override def beforeAll(): Unit = {
    super.beforeAll()

    TestCardanoBlockRepository.createTables()
  }

  "publish" should {
    val atalaObject = node_internal
      .AtalaObject()
      .withBlock(node_internal.AtalaObject.Block.BlockContent(node_internal.AtalaBlock().withVersion("1")))
    val expectedWalletApiPath = s"v2/wallets/$walletId/transactions"

    "publish an object" in {
      val cardanoWalletApiClient = FakeCardanoWalletApiClient.Success(
        expectedWalletApiPath,
        readResource("publishReference_cardanoWalletApiRequest.json"),
        readResource("publishReference_success_cardanoWalletApiResponse.json")
      )
      val cardanoLedgerService = createCardanoLedgerService(cardanoWalletApiClient)

      // Only test that it doesn't fail, as calling the wrong endpoint with the wrong params fails
      cardanoLedgerService.publish(atalaObject).futureValue
    }

    "fail with invalid transaction" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Fail(
          expectedWalletApiPath,
          readResource("publishReference_cardanoWalletApiRequest.json"),
          "internal",
          "Internal error"
        )
      val cardanoLedgerService = createCardanoLedgerService(cardanoWalletApiClient)

      val error = intercept[RuntimeException] {
        cardanoLedgerService.publish(atalaObject).futureValue
      }

      error.getCause.getMessage must be("FATAL: Error while publishing reference: InvalidTransaction")
    }
  }

  "syncAtalaObjects" should {
    "sync Atala objects" in {
      val totalBlockCount = 50
      TestCardanoBlockRepository.createRandomBlocks(totalBlockCount).foreach(TestCardanoBlockRepository.insertBlock)
      val cardanoLedgerService = createCardanoLedgerService(FakeCardanoWalletApiClient.NotFound())

      cardanoLedgerService.syncAtalaObjects().futureValue

      // TODO: Test Atala objects processed instead.
      val lastSyncedBlockNo = keyValueService.getInt(LAST_SYNCED_BLOCK_NO).futureValue.value
      // The last `blockConfirmationsToWait` blocks have not been confirmed, and should not been processed
      lastSyncedBlockNo must be(totalBlockCount - blockConfirmationsToWait)
    }

    // TODO: Test **only new** Atala objects are processed (start with a high `LAST_SYNCED_BLOCK_NO`).
  }

  private def createCardanoLedgerService(
      cardanoWalletApiClient: CardanoWalletApiClient,
      onAtalaObject: AtalaObjectNotificationHandler = noOpObjectHandler
  ): CardanoLedgerService = {
    val cardanoClient = createCardanoClient(cardanoWalletApiClient)
    new CardanoLedgerService(
      CardanoNetwork.Testnet,
      walletId,
      walletPassphrase,
      paymentAddress,
      blockConfirmationsToWait,
      cardanoClient,
      keyValueService,
      onAtalaObject,
      scheduler
    )
  }

  private def createCardanoClient(cardanoWalletApiClient: CardanoWalletApiClient): CardanoClient = {
    new CardanoClient(new CardanoDbSyncClient(new CardanoBlockRepository(database)), cardanoWalletApiClient)
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"services/cardano/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
