package io.iohk.atala.prism.node.services

import java.time.Instant
import java.time.temporal.ChronoUnit
import cats.effect.IO
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.node.NodeConfig
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models.{Address, WalletId}
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.node.services.models.testing.TestAtalaHandlers
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.utils.IOUtils._
import monix.execution.schedulers.TestScheduler
import org.scalatest.Ignore
import org.scalatest.OptionValues._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import tofu.logging.Logs

import scala.concurrent.duration._

// Todo make CardanoLedgerServiceIntegrationSpec great again
//  when https://input-output.atlassian.net/browse/ATA-5337 done or 1-2 released
@Ignore
class CardanoLedgerServiceIntegrationSpec extends AtalaWithPostgresSpec with CatsEffectBase {
  private val logs = Logs.withContext[IO, IOWithTraceIdContext]
  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  private val LONG_TIMEOUT = Timeout(1.minute)
  private val RETRY_TIMEOUT = 2.minutes
  private val RETRY_SLEEP = 10.seconds

  private val scheduler: TestScheduler = TestScheduler()

  "CardanoLedgerService" should {
    "notify on published PRISM transactions" in {
      assume(
        shouldTestCardanoIntegration(),
        "The integration test was cancelled because it hasn't been configured"
      )

      // Set up
      val clientConfig =
        NodeConfig.cardanoConfig(ConfigFactory.load().getConfig("cardano"))
      val walletId = WalletId.from(clientConfig.walletId).value
      val paymentAddress = Address(clientConfig.paymentAddress)
      val (cardanoClient, releaseCardanoClient) =
        CardanoClient
          .makeResource(clientConfig.cardanoClientConfig, logs)
          .allocated
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
      val keyValueService = KeyValueService.unsafe(KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, logs), logs)
      val notificationHandler = new TestAtalaHandlers()

      val cardanoLedgerService = new CardanoLedgerService(
        CardanoNetwork.Testnet,
        walletId,
        clientConfig.walletPassphrase,
        paymentAddress,
        blockNumberSyncStart = 0,
        // Do not wait on blocks becoming confirmed
        blockConfirmationsToWait = 0,
        cardanoClient,
        keyValueService,
        notificationHandler.asCardanoBlockHandler,
        notificationHandler.asAtalaObjectHandler,
        scheduler
      )

      // Avoid syncing pre-existing blocks
      val latestBlock =
        cardanoClient.getLatestBlock(TraceId.generateYOLO).unsafeToFuture().futureValue(LONG_TIMEOUT).toOption.value
      keyValueService
        .set(LAST_SYNCED_BLOCK_NO, Some(latestBlock.header.blockNo))
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // Publish random object
      val atalaObject = node_internal
        .AtalaObject()
        .withBlockContent(node_internal.AtalaBlock(operations = Seq()))
      val transaction = cardanoLedgerService
        .publish(atalaObject)
        .futureValue(LONG_TIMEOUT)
        .toOption
        .value
        .transaction
      println(
        s"AtalaObject published in transaction ${transaction.transactionId} on ${transaction.ledger}"
      )

      def notifiedAtalaObjects: Seq[node_internal.AtalaObject] = {
        notificationHandler.receivedNotifications.map(_.atalaObject).toSeq
      }

      // Wait for the transaction to become available in cardano-node
      val retryEndTime =
        Instant.now.plus(RETRY_TIMEOUT.toMillis, ChronoUnit.MILLIS)
      while (
        Instant.now
          .isBefore(retryEndTime) && !notifiedAtalaObjects.contains(atalaObject)
      ) {
        Thread.sleep(RETRY_SLEEP.toMillis)
        // Sync objects
        cardanoLedgerService.syncAtalaObjects().futureValue(LONG_TIMEOUT)
      }

      // Verify object has been notified
      notifiedAtalaObjects must contain(atalaObject)
      releaseCardanoClient.run(TraceId.generateYOLO).unsafeRunSync()
    }
  }

  /** Returns whether Cardano Integration tests should run because it's running in CI, or it's locally configured.
    */
  private def shouldTestCardanoIntegration(): Boolean = {
    // Return true when CI="true" (environment is expected to be configured), or NODE_CARDANO_WALLET_ID is defined
    // (any other Cardano variable could be used, this one is arbitrary)
    sys.env
      .get("CI")
      .filter(_ == "true")
      .orElse(sys.env.get("NODE_CARDANO_WALLET_ID"))
      .isDefined
  }
}
