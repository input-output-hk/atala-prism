package io.iohk.atala.prism.node.cardano

import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.iohk.atala.prism.node.NodeConfig
import io.iohk.atala.prism.node.cardano.models.{Address, Lovelace, Payment, TransactionMetadata, WalletId}
import monix.execution.Scheduler.Implicits.global
import org.scalatest.EitherValues._
import org.scalatest.WordSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration._

class CardanoClientExample extends WordSpec {

  private val globalConfig = ConfigFactory.load()

  private lazy val clientConfig = NodeConfig.cardanoConfig(globalConfig.getConfig("cardano"))

  "CardanoClient example" should {
    "be able to access db sync and wallet" in {
      assume(shouldTestCardanoIntegration(), "The integration test was cancelled because it hasn't been configured")

      val client = CardanoClient(clientConfig.cardanoClientConfig)
      val walletId = clientConfig.walletId
      val passphrase = clientConfig.walletPassphrase
      val address = clientConfig.paymentAddress

      client.getLatestBlock().value.futureValue(Timeout(1.minute)).right.value

      val payments = List(
        Payment(Address(address), Lovelace(1000000))
      )
      val metadata = TransactionMetadata(
        Json.obj(
          "0" -> Json.obj(
            "string" -> Json.fromString("foo bar"),
            "bytes" -> Json.fromString("0x1234567890abcdef"),
            "int" -> Json.fromInt(0),
            "int-array" -> Json.arr(Json.fromInt(0), Json.fromInt(1), Json.fromInt(2))
          )
        )
      )

      client
        .postTransaction(WalletId.from(walletId).get, payments, Some(metadata), passphrase)
        .value
        .futureValue(Timeout(1.minute))
        .right
        .value
    }
  }

  /**
    * Returns whether Cardano Integration tests should run because it's running in CI, or it's locally configured.
    */
  private def shouldTestCardanoIntegration(): Boolean = {
    // Return true when CI="true" (environment is expected to be configured), or GEUD_NODE_CARDANO_WALLET_ID is defined
    // (any other Cardano variable could be used, this one is arbitrary)
    sys.env.get("CI").filter(_ == "true").orElse(sys.env.get("GEUD_NODE_CARDANO_WALLET_ID")).isDefined
  }
}
