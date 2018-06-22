package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.util.Random

class SingleMessageSimulation extends Simulation {

  private val feeder: Iterator[Map[String, String]] =
    Iterator.continually(Map("message" -> Random.alphanumeric.take(256).mkString))

  private val sendMessage: ChainBuilder =
    feed(feeder).exec(
      http("SendMessage")
        .post("/message")
        .body(StringBody("""{"message":"${message}", "expectedPeerCount":7}""")))

  private val messageScenario =
    scenario(s"Message scenario").exec(sendMessage)

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(messageScenario.
    inject(atOnceUsers(1))).
    protocols(httpConf)
}
