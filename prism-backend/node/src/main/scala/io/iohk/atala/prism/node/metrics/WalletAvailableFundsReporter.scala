package io.iohk.atala.prism.node.metrics

import com.typesafe.config.Config
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models.WalletId
import io.iohk.atala.prism.node.services.CardanoLedgerService
import kamon.Kamon
import kamon.metric.{Gauge, PeriodSnapshot}
import kamon.module.MetricReporter

import scala.concurrent.ExecutionContext

class WalletAvailableFundsReporter(walletId: WalletId, cardanoClient: CardanoClient)(implicit
    ec: ExecutionContext
) extends MetricReporter {
  val walletFunds: Gauge = Kamon.gauge("node.wallet.available.funds").withoutTags()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    cardanoClient
      .getWalletDetails(walletId)
      .value
      .foreach(_.foreach(details => walletFunds.update(details.balance.available.doubleValue)))

  override def stop(): Unit = ()

  override def reconfigure(newConfig: Config): Unit = ()
}

object WalletAvailableFundsReporter {
  def apply(config: CardanoLedgerService.Config, cardanoClient: CardanoClient)(implicit
      ec: ExecutionContext
  ): WalletAvailableFundsReporter = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(throw new IllegalArgumentException(s"Wallet ID ${config.walletId} is invalid"))

    new WalletAvailableFundsReporter(walletId, cardanoClient)
  }
}
