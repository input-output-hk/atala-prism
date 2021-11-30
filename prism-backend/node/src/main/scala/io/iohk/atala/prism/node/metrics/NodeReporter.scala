package io.iohk.atala.prism.node.metrics

import cats.effect.unsafe.IORuntime
import com.typesafe.config.Config
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.cardano.models.WalletId
import io.iohk.atala.prism.node.cardano.{CardanoClient, LAST_SYNCED_BLOCK_NO}
import io.iohk.atala.prism.node.services.{CardanoLedgerService, KeyValueService}
import io.iohk.atala.prism.tracing.Tracing._
import tofu.logging.{Logs, Logging}
import cats.effect.IO
import kamon.Kamon
import kamon.metric.{Gauge, PeriodSnapshot}
import kamon.module.MetricReporter

import scala.concurrent.ExecutionContext
import scala.util.Try

class NodeReporter(
    walletId: WalletId,
    cardanoClient: CardanoClient[IOWithTraceIdContext],
    keyValueService: KeyValueService[IOWithTraceIdContext],
    blockNumberSyncStart: Int,
    logger: Logging[IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends MetricReporter {

  private val nextBlockToSyncGauge: Gauge =
    Kamon.gauge("node.next.block.to.sync.by.prism").withoutTags()
  private val lastSyncedBlockByWallet: Gauge =
    Kamon.gauge("node.last.synced.block.by.wallet").withoutTags()
  private val walletFunds: Gauge =
    Kamon.gauge("node.wallet.available.funds").withoutTags()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    postNextBlockToSync()
    postWalletLastBlock()
    reportWalletFunds()
  }

  override def stop(): Unit = ()

  override def reconfigure(newConfig: Config): Unit = ()

  private def reportWalletFunds(): Unit = trace { traceId =>
    cardanoClient
      .getWalletDetails(walletId)
      .run(traceId)
      .unsafeToFuture()
  }
    .foreach(
      _.foreach(details => walletFunds.update(details.balance.available.doubleValue))
    )

  private def postNextBlockToSync(): Unit = trace { traceId =>
    keyValueService
      .getInt(LAST_SYNCED_BLOCK_NO)
      .run(traceId)
      .unsafeToFuture()
  }
    .foreach { maybeNumber =>
      val nextBlockToSync = CardanoLedgerService.calculateLastSyncedBlockNo(
        maybeNumber,
        blockNumberSyncStart
      ) + 1
      updateGauge(nextBlockToSyncGauge, nextBlockToSync)
    }

  private def postWalletLastBlock(): Unit = trace { traceId =>
    cardanoClient.getLatestBlock
      .run(traceId)
      .unsafeToFuture()
  }
    .foreach(
      _.foreach(block => updateGauge(lastSyncedBlockByWallet, block.header.blockNo))
    )

  private def updateGauge(gauge: Gauge, newValue: Int): Unit =
    Try(gauge.update(newValue.doubleValue())).toEither.left.foreach(er =>
      logger.error(s"${gauge.metric.name} just blew up", er)
    )
}

object NodeReporter {
  def apply(
      config: CardanoLedgerService.Config,
      cardanoClient: CardanoClient[IOWithTraceIdContext],
      keyValueService: KeyValueService[IOWithTraceIdContext],
      logs: Logs[IO, IOWithTraceIdContext]
  )(implicit ec: ExecutionContext, runtime: IORuntime): NodeReporter = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Wallet ID ${config.walletId} is invalid"
        )
      )

    new NodeReporter(
      walletId,
      cardanoClient,
      keyValueService,
      config.blockNumberSyncStart,
      logger = logs.forService[NodeReporter].unsafeRunSync()
    )
  }
}
