package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class MessageRateSimulation extends Simulation {

  private val messageLength = 256
  private val maxUsersPerSec = 500

  private val feeder: Iterator[Map[String, String]] =
    Iterator.continually(
      Map("message" -> Random.alphanumeric.take(messageLength).mkString))

  private val sendMessage: ChainBuilder =
    feed(feeder).exec(
      http("SendMessage")
        .post("/message")
        .body(StringBody("""{"message":"${message}"}""")))

  private val singleMessageScenario =
    scenario(s"Max Users / sec = $maxUsersPerSec, messageLength = $messageLength").
      exec(sendMessage)

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(singleMessageScenario.
    inject(Range.inclusive(1, maxUsersPerSec, 10).map(i => constantUsersPerSec(i) during (2 seconds))).
    protocols(httpConf))
}
