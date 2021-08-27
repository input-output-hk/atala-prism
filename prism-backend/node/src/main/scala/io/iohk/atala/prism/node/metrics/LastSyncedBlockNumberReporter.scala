package io.iohk.atala.prism.node.metrics

import com.typesafe.config.Config
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_NO
import io.iohk.atala.prism.node.services.KeyValueService
import kamon.Kamon
import kamon.metric.{Gauge, PeriodSnapshot}
import kamon.module.MetricReporter

import scala.concurrent.ExecutionContext

class LastSyncedBlockNumberReporter(keyValueService: KeyValueService)(implicit ec: ExecutionContext)
    extends MetricReporter {
  val lastSyncedBlockNumber: Gauge = Kamon.gauge("node.last.synced.block.number").withoutTags()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    keyValueService
      .getInt(LAST_SYNCED_BLOCK_NO)
      .foreach(_.foreach(number => lastSyncedBlockNumber.update(number.doubleValue())))

  override def stop(): Unit = ()

  override def reconfigure(newConfig: Config): Unit = ()
}
