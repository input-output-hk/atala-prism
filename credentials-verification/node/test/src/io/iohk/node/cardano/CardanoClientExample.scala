package io.iohk.node.cardano

import com.typesafe.config.ConfigFactory
import io.iohk.node.NodeConfig
import io.iohk.node.cardano.models.{Address, Lovelace, Payment, WalletId}
import monix.execution.Scheduler.Implicits.global
import org.scalatest.EitherValues._
import org.scalatest.WordSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration._

class CardanoClientExample extends WordSpec {

  val globalConfig = ConfigFactory.load()

  val clientConfig = NodeConfig.cardanoConfig(globalConfig.getConfig("cardano"))

  "CardanoClient example" should {
    "should be able to access db sync and wallet" in {
      val client = CardanoClient(clientConfig.cardanoClientConfig)
      val walletId = clientConfig.walletId
      val passphrase = clientConfig.walletPassphrase
      val address = clientConfig.paymentAddress

      client.getLatestBlock().value.futureValue(Timeout(1.minute)).right.value

      val payments = List(
        Payment(Address(address), Lovelace(1))
      )

      client
        .postTransaction(WalletId.from(walletId).get, payments, passphrase)
        .value
        .futureValue(Timeout(1.minute))
        .right
        .value
    }
  }
}
