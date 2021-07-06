package io.iohk.atala.mirror.config

import com.typesafe.config.Config

sealed abstract class CardanoNetwork(val name: String)
object CardanoNetwork {
  case object TestNet extends CardanoNetwork("testnet")
  case object MainNet extends CardanoNetwork("mainnet")
}

case class CardanoConfig(network: CardanoNetwork, addressCount: Int, syncIntervalInSeconds: Int)

object CardanoConfig {

  def apply(globalConfig: Config): CardanoConfig = {
    val config = globalConfig.getConfig("cardano")

    val network: CardanoNetwork = config.getString("network") match {
      case "mainmet" => CardanoNetwork.MainNet
      case "testnet" => CardanoNetwork.TestNet
      case _ => throw new RuntimeException("Invalid Cardano network.")
    }
    val addressCount = config.getInt("addressCount")
    val syncIntervalInSeconds = config.getInt("blockchainSyncIntervalSeconds")

    CardanoConfig(
      network = network,
      addressCount = addressCount,
      syncIntervalInSeconds = syncIntervalInSeconds
    )
  }

}
