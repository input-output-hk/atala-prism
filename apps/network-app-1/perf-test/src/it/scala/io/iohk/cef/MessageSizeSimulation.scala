package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.util.Random

class MessageSizeSimulation extends Simulation {

  private val usersPerSec = 1
  private val maxMessageSize: Int = 1024 * 25

  private val feeder: Iterator[Map[String, String]] =
    Range.inclusive(1, maxMessageSize).map(messageLength =>
      Map(
        "messageLength" -> messageLength.toString,
        "message" -> Random.alphanumeric.take(messageLength).mkString)).toIterator

  private val sendMessage: ChainBuilder =
    feed(feeder).exec(
      http("SendMessage")
        .post("/message")
        .body(StringBody("""{"message":"${message}"}""")))

  private val messageScenario =
    scenario(s"Users / sec = $usersPerSec").
      repeat(maxMessageSize, "n")(exec(sendMessage))

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(messageScenario.
    inject(atOnceUsers(usersPerSec))).
    protocols(httpConf)
}
