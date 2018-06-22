package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class MessageRateSimulation extends Simulation {

  private val messageLength = 256

  private val minUsersPerSec = 0
  private val maxUsersPerSec = 1000
  private val steps = 25
  private val dy = (maxUsersPerSec - minUsersPerSec) / steps
  private val burstDuration = 3

  private val feeder: Iterator[Map[String, String]] =
    (for {
      messageRate <- Range.inclusive(minUsersPerSec, maxUsersPerSec, dy)
      _ <- Range.inclusive(0, burstDuration*messageRate-1)
    } yield Map(
      "messageRate" -> messageRate.toString,
      "message" -> Random.alphanumeric.take(messageLength).mkString)).toIterator

  private val sendMessage: ChainBuilder =
    feed(feeder).exec(
      http("SendMessage (rate=${messageRate})")
        .post("/message")
        .body(StringBody("""{"message":"${message}", "expectedPeerCount":7}""")))

  private val messageScenario: ScenarioBuilder =
    scenario(s"Message rate scenario").
      exec(sendMessage)

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(messageScenario.
    inject(Range.inclusive(minUsersPerSec, maxUsersPerSec, dy).map(i => constantUsersPerSec(i) during (burstDuration seconds))).
    protocols(httpConf))
}
