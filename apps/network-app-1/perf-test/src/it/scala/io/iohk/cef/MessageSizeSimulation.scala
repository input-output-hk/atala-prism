package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.util.Random

class MessageSizeSimulation extends Simulation {

  private val steps = 25
  private val minMessageSize = 1024
  private val maxMessageSize = 1024 * 50
  private val dy = (maxMessageSize - minMessageSize) / steps

  private val feeder: Iterator[Map[String, String]] =
    (for {
      messageSize <- Range.inclusive(minMessageSize, maxMessageSize, dy)
      _ <- Range.inclusive(0, 99)
    } yield Map(
      "messageSize" -> messageSize.toString,
      "message" -> Random.alphanumeric.take(messageSize).mkString)).toIterator

  private val sendMessage: ChainBuilder =
    feed(feeder).exec(
      http("SendMessage (sz=${messageSize})")
        .post("/message")
        .body(StringBody("""{"message":"${message}", "expectedPeerCount":7}""")))

  private val messageScenario =
    scenario(s"Message size scenario").
      repeat(steps, "step")(repeat(100, "loop")(exec(sendMessage)))

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(messageScenario.
    inject(atOnceUsers(1))).
    protocols(httpConf)
}
