package io.iohk.cef

import java.time.Clock

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import io.iohk.cef.ConfigExtensions._
import io.iohk.cef.db.KnownNodeStorageImpl
import io.iohk.cef.demo.SimpleNode3
import io.iohk.cef.discovery.DiscoveryConfig

class NetworkApp1(config: Config) {

  private implicit val untypedSystem = ActorSystem("network-app-1")
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = untypedSystem.dispatcher

  type NameHostPort = (String, String, Int)

  val discoveryConfig = DiscoveryConfig(config)

  val knownNodeStorage = new KnownNodeStorageImpl(Clock.systemUTC())

  // either specify name/host/port and let the node generate a key
  val ephemeralConfig: Option[SimpleNode3] = for {
    nodeName <- config.getOption(_.getString("node-name"))
    serverHost <- config.getOption(_.getString("server-host"))
    serverPort <- config.getOption(_.getInt("server-port"))
  } yield SimpleNode3(nodeName, serverHost, serverPort, discoveryConfig, knownNodeStorage)

  val fixedKeyConfig: Option[SimpleNode3] = for {
    nodeKey <- config.getOption(_.getString("node-key"))
    serverHost <- config.getOption(_.getString("server-host"))
    serverPort <- config.getOption(_.getInt("server-port"))
  } yield SimpleNode3(serverHost, serverPort, nodeKey, discoveryConfig, knownNodeStorage)


  // or explicitly specify a node URI with a key in the user info
  val node: SimpleNode3 = ephemeralConfig.getOrElse(fixedKeyConfig.get)

  val behavior = BehaviorRoot.start(
    node = node,
    gatewayHost = config.getString("gateway-host"),
    gatewayPort = config.getInt("gateway-port"))

  untypedSystem.spawn(behavior, "NetworkAppActor")
}

object NetworkApp1 extends App {
  new NetworkApp1(ConfigFactory.load)
}
