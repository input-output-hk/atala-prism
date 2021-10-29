package io.iohk.atala.prism.metrics

import com.typesafe.config.Config
import kamon.Kamon
import kamon.metric.{Counter, PeriodSnapshot}
import kamon.module.MetricReporter

import scala.util.Try

class UptimeReporter(globalConfig: com.typesafe.config.Config) extends MetricReporter {
  val uptimeMetric: Counter = Kamon.counter("jvm.uptime.seconds").withoutTags()
  // 15 is fallback, it's better to have something than nothing
  val tickTimeInSeconds: Long = Try(
    globalConfig.getDuration("kamon.metric.tick-interval").getSeconds
  ).getOrElse(15)

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    uptimeMetric.increment(tickTimeInSeconds)
    ()
  }

  override def stop(): Unit = ()

  override def reconfigure(newConfig: Config): Unit = ()
}
