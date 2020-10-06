package io.iohk.atala.prism.node.services

import com.google.protobuf.ByteString
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.node.NodeConfig
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models.{Address, WalletId}
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.node.services.models.testing.TestAtalaObjectNotificationHandler
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.prism.protos.node_internal
import monix.execution.schedulers.TestScheduler
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration._
import scala.util.Random

class CardanoLedgerServiceIntegrationSpec extends PostgresRepositorySpec {
  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  private val LONG_TIMEOUT = Timeout(1.minute)

  private val scheduler: TestScheduler = TestScheduler()

  "CardanoLedgerService" should {
    "notify on published PRISM transactions" in {
      assume(shouldTestCardanoIntegration(), "The integration test was cancelled because it hasn't been configured")

      // Set up
      val clientConfig = NodeConfig.cardanoConfig(ConfigFactory.load().getConfig("cardano"))
      val walletId = WalletId.from(clientConfig.walletId).value
      val paymentAddress = Address(clientConfig.paymentAddress)
      val cardanoClient = CardanoClient(clientConfig.cardanoClientConfig)
      val keyValueService = new KeyValueService(new KeyValuesRepository(database))
      val notificationHandler = new TestAtalaObjectNotificationHandler()
      val cardanoLedgerService = new CardanoLedgerService(
        CardanoNetwork.Testnet,
        walletId,
        clientConfig.walletPassphrase,
        paymentAddress,
        // Do not wait on blocks becoming confirmed
        blockConfirmationsToWait = 0,
        cardanoClient,
        keyValueService,
        notificationHandler.asHandler,
        scheduler
      )

      // Avoid syncing pre-existing blocks
      val latestBlock = cardanoClient.getLatestBlock().value.futureValue(LONG_TIMEOUT).right.value
      keyValueService.set(LAST_SYNCED_BLOCK_NO, Some(latestBlock.header.blockNo)).futureValue

      // Publish random object
      val atalaObject = node_internal
        .AtalaObject()
        .withBlock(
          node_internal.AtalaObject.Block.BlockHash(ByteString.copyFrom(random32Bytes()))
        )
      cardanoLedgerService.publish(atalaObject).futureValue(LONG_TIMEOUT)

      def notifiedAtalaObjects: Seq[node_internal.AtalaObject] = {
        notificationHandler.receivedNotifications.map(_.atalaObject)
      }

      // Wait up to a minute for the transaction to become available in cardano-node
      var retries = 6
      while (retries > 0 && !notifiedAtalaObjects.contains(atalaObject)) {
        Thread.sleep(10.seconds.toMillis)
        // Sync objects
        cardanoLedgerService.syncAtalaObjects().futureValue(LONG_TIMEOUT)
        retries -= 1
      }

      // Verify object has been notified
      notifiedAtalaObjects must contain(atalaObject)
    }
  }

  private def random32Bytes(): Array[Byte] = {
    val bytes = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    bytes
  }

  /**
    * Returns whether Cardano Integration tests should run because it's running in CI, or it's locally configured.
    */
  private def shouldTestCardanoIntegration(): Boolean = {
    // Return true when CI="true" (environment is expected to be configured), or NODE_CARDANO_WALLET_ID is defined
    // (any other Cardano variable could be used, this one is arbitrary)
    sys.env.get("CI").filter(_ == "true").orElse(sys.env.get("NODE_CARDANO_WALLET_ID")).isDefined
  }
}
