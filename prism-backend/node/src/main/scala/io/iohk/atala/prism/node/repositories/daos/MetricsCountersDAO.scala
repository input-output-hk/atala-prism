package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import cats.syntax.functor._

object MetricsCountersDAO {
  def incrementCounter(counterName: String): ConnectionIO[Unit] =
    sql"""
         |INSERT INTO metrics_counters
         |VALUES ($counterName, 1)
         |ON CONFLICT (counter_name) DO UPDATE SET counter_value = metrics_counters.counter_value + 1
         |""".stripMargin.update.run.void

  def getCounter(counterName: String): ConnectionIO[Int] =
    sql"""
         |SELECT counter_value FROM metrics_counters WHERE counter_name = $counterName
       """.stripMargin.query[Int].option.map(_.getOrElse(0))
}
