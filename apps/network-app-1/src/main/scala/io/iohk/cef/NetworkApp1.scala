package io.iohk.cef


import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._

import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import io.iohk.cef.demo.SimpleNode3
import io.iohk.cef.demo.SimpleNode3._

import ConfigExtensions._

object NetworkApp1 extends App {

  implicit val untypedSystem = ActorSystem("network-app-1")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = untypedSystem.dispatcher

  val config = ConfigFactory.load

  val gatewayHost = config.getString("gateway-host")
  val gatewayPort = config.getInt("gateway-port")
  val serverHost = config.getString("server-host")
  val serverPort = config.getInt("server-port")
  val nodeName = config.getString("node-name")
  val bootstrapPeer = config.getOption(_.getURI("bootstrap-peer"))

  val start: Behavior[String] = Behaviors.setup {
    context =>

      val nodeActor: ActorRef[NodeCommand] =
        context.spawn(new SimpleNode3(nodeName, serverHost, serverPort, None).server, "NodeActor")

      val httpGatewayRoute: Route = HttpGateway.route(
        request => nodeActor ! Send(request.message))

      val startupListener: Behavior[Started] = Behaviors.receiveMessage {
        case Started(nodeUri) =>

          Http().bindAndHandle(httpGatewayRoute, gatewayHost, gatewayPort.toInt)

          Behavior.ignore
      }

      nodeActor ! Start(context.spawn(startupListener, "Node_Startup_Listener"))

      Behaviors.receiveMessage(_ => Behavior.ignore)
  }

  untypedSystem.spawn(start, "NetworkAppActor")
}
