package io.iohk.atala.prism.node

import com.typesafe.config.Config
import io.iohk.cvp.repositories.TransactorFactory
import io.iohk.atala.prism.node.bitcoin.BitcoinClient
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.services.CardanoLedgerService

object NodeConfig {

  def bitcoinConfig(config: Config): BitcoinClient.Config = {
    val host = config.getString("host")
    val port = config.getInt("port")
    val username = config.getString("username")
    val password = config.getString("password")
    BitcoinClient.Config(host, port, username, password)
  }

  def cardanoConfig(config: Config): CardanoLedgerService.Config = {
    val walletId = config.getString("walletId")
    val walletPassphrase = config.getString("walletPassphrase")
    val paymentAddress = config.getString("paymentAddress")
    val blockConfirmationsToWait = config.getInt("blockConfirmationsToWait")
    val dbSyncConfig = cardanoDbSyncConfig(config.getConfig("dbSync"))
    val walletConfig = cardanoWalletConfig(config.getConfig("wallet"))
    CardanoLedgerService.Config(
      walletId,
      walletPassphrase,
      paymentAddress,
      blockConfirmationsToWait,
      CardanoClient.Config(dbSyncConfig, walletConfig)
    )
  }

  def cardanoDbSyncConfig(config: Config): CardanoDbSyncClient.Config = {
    CardanoDbSyncClient.Config(transactorConfig(config.getConfig("db")))
  }

  def transactorConfig(config: Config): TransactorFactory.Config = {
    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password
    )
  }

  def cardanoWalletConfig(config: Config): CardanoWalletApiClient.Config = {
    val host = config.getString("host")
    val port = config.getInt("port")
    CardanoWalletApiClient.Config(host, port)
  }
}
