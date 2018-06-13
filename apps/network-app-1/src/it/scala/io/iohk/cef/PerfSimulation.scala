package io.iohk.cef

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.iohk.cef.SendMessage.sendMessage

import scala.concurrent.duration._
import scala.util.Random

object SendMessage {

  private val messageLength = 256

  private val feeder = Iterator.continually(
    Map("message" -> Random.alphanumeric.take(messageLength).mkString))

  val sendMessage =
    feed(feeder).exec(
      http("SendMessage")
        .post("/message")
        .body(StringBody("""{"message":"${message}"}""")))
}

class PerfSimulation extends Simulation {

  private val singleMessageScenario =
    scenario("Simple message sending scenario").
      exec(sendMessage)

  private val httpConf = http
    .baseURL("http://localhost:8000")
    .contentTypeHeader("application/json; charset=utf-8")
    .acceptHeader("application/json")

  setUp(singleMessageScenario.
    inject(Range.inclusive(1, 10).map(i => constantUsersPerSec(i) during(2 seconds))).
    protocols(httpConf))
}
