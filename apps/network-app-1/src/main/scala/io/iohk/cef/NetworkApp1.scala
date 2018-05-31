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

// Example command line
// Create a standalone node
// -Dnode-name=A -Dgateway-host=localhost -Dgateway-port=8080 -Dserver-host=localhost -Dserver-port=3000
//
// Create an additional node that references the first as a bootstrap peer.
// -Dnode-name=B -Dgateway-host=localhost -Dgateway-port=9090 -Dserver-host=localhost -Dserver-port=4000 -Dbootstrap-peer=enode://6fc6fb2f1c9ec87825059bca627dd78698a8142ea07c9019668e06df37b3079e970776797cf90d9d847d8ab708566158f307917f184c1de228c786a7109c28bb@localhost:3000
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
