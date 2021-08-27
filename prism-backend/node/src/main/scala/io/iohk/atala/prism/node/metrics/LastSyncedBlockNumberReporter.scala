package io.iohk.atala.prism.node.metrics

import com.typesafe.config.Config
import io.iohk.atala.prism.node.cardano.{CardanoClient, LAST_SYNCED_BLOCK_NO}
import io.iohk.atala.prism.node.services.{CardanoLedgerService, KeyValueService}
import kamon.Kamon
import kamon.metric.{Gauge, PeriodSnapshot}
import kamon.module.MetricReporter
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.Try

class LastSyncedBlockNumberReporter(
    cardanoClient: CardanoClient,
    keyValueService: KeyValueService,
    blockNumberSyncStart: Int
)(implicit
    ec: ExecutionContext
) extends MetricReporter {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val nextBlockToSyncGauge: Gauge = Kamon.gauge("node.next.block.to.sync.by.prism").withoutTags()
  private val lastSyncedBlockByWallet: Gauge = Kamon.gauge("node.last.synced.block.by.wallet").withoutTags()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    postNextBlockToSync()
    postWalletLastBlock()
  }

  override def stop(): Unit = ()

  override def reconfigure(newConfig: Config): Unit = ()

  private def postNextBlockToSync(): Unit =
    keyValueService
      .getInt(LAST_SYNCED_BLOCK_NO)
      .foreach { maybeNumber =>
        val nextBlockToSync = CardanoLedgerService.calculateLastSyncedBlockNo(maybeNumber, blockNumberSyncStart) + 1
        updateGauge(nextBlockToSyncGauge, nextBlockToSync)
      }

  private def postWalletLastBlock(): Unit = {
    cardanoClient
      .getLatestBlock()
      .value
      .foreach(_.foreach(block => updateGauge(lastSyncedBlockByWallet, block.header.blockNo)))
  }

  private def updateGauge(gauge: Gauge, newValue: Int): Unit =
    Try(gauge.update(newValue.doubleValue())).toEither.left.foreach(er =>
      logger.error(s"${gauge.metric.name} just blew up", er)
    )
}

object LastSyncedBlockNumberReporter {
  def apply(cardanoClient: CardanoClient, keyValueService: KeyValueService, config: CardanoLedgerService.Config)(
      implicit ec: ExecutionContext
  ): LastSyncedBlockNumberReporter =
    new LastSyncedBlockNumberReporter(cardanoClient, keyValueService, config.blockNumberSyncStart)
}
