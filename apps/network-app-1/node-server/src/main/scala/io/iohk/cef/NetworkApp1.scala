package io.iohk.cef

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import io.iohk.cef.ConfigExtensions._

class NetworkApp1(config: Config) {

  implicit val untypedSystem = ActorSystem("network-app-1")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = untypedSystem.dispatcher

  val behavior = BehaviorRoot.start(
    config.getString("node-name"),
    config.getString("server-host"), config.getInt("server-port"),
    config.getString("gateway-host"), config.getInt("gateway-port"),
    config.getOption(_.getURI("bootstrap-peer")))

  untypedSystem.spawn(behavior, "NetworkAppActor")
}

object NetworkApp1 extends App {
  new NetworkApp1(ConfigFactory.load)
}
