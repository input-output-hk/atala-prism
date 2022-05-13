package io.iohk.atala.prism.node.repositories.daos

import cats.effect.unsafe.implicits.global
import doobie.implicits._

import io.iohk.atala.prism.AtalaWithPostgresSpec

class MetricsCountersDAOSpec extends AtalaWithPostgresSpec {

  private val metricCounters: List[(String, Int)] = List(
    ("number_of_created_dids", 4),
    ("number_of_issued_credential_batches", 0),
    ("number_of_protocol_updates", 5),
    ("number_of_revoked_credentials", 2),
    ("number_of_did_updates", 5)
  )

  "MetricsCountersDAO.incrementCounter" should {
    "increment counter" in {
      metricCounters.foreach { case (metricName, metricCounter) =>
        (0 until metricCounter).foreach { _ =>
          MetricsCountersDAO.incrementCounter(metricName).transact(database).unsafeRunSync()
        }
      }

      metricCounters.foreach { case (metricName, metricCounter) =>
        val counter = MetricsCountersDAO.getCounter(metricName).transact(database).unsafeRunSync()
        counter must be(metricCounter)
      }
    }
  }
}
