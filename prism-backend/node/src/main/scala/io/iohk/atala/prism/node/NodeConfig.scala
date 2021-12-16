package io.iohk.atala.prism.node

import cats.Applicative
import com.typesafe.config.Config
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.services.CardanoLedgerService
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.repositories.TransactorFactory
import sttp.model.Header

import scala.util.Try

object NodeConfig {

  def cardanoConfig(config: Config): CardanoLedgerService.Config = {
    val network =
      CardanoNetwork.withNameInsensitive(config.getString("network"))
    val walletId = config.getString("walletId")
    val walletPassphrase = config.getString("walletPassphrase")
    val paymentAddress = config.getString("paymentAddress")
    val blockNumberSyncStart = config.getInt("blockNumberSyncStart")
    val blockConfirmationsToWait = config.getInt("blockConfirmationsToWait")
    val dbSyncConfig = cardanoDbSyncConfig(config.getConfig("dbSync"))
    val walletConfig = cardanoWalletConfig(config.getConfig("wallet"))
    CardanoLedgerService.Config(
      network,
      walletId,
      walletPassphrase,
      paymentAddress,
      blockNumberSyncStart,
      blockConfirmationsToWait,
      CardanoClient.Config(dbSyncConfig, walletConfig)
    )
  }

  def cardanoDbSyncConfig(config: Config): CardanoDbSyncClient.Config = {
    CardanoDbSyncClient.Config(TransactorFactory.transactorConfig(config))
  }

  def cardanoWalletConfig(config: Config): CardanoWalletApiClient.Config = {
    val host = config.getString("host")
    val port = config.getInt("port")
    val routingHeaderName = Try("routingHeaderName").map(config.getString).toOption
    val routingHeaderValue = Try("routingHeaderValue").map(config.getString).toOption
    val routingHeader = Applicative[Option]
      .map2(routingHeaderName, routingHeaderValue)((headerName, headerValue) => Header(headerName, headerValue))
    CardanoWalletApiClient.Config(host, port, routingHeader)
  }
}
