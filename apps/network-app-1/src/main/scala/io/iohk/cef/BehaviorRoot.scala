package io.iohk.cef

import java.net.URI

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult._
import akka.stream.Materializer
import io.iohk.cef.demo.SimpleNode3
import io.iohk.cef.demo.SimpleNode3.{NodeCommand, Send, Start, Started}

import scala.concurrent.ExecutionContext

object BehaviorRoot {
  def start(nodeName: String,
            serverHost: String,
            serverPort: Int,
            gatewayHost: String,
            gatewayPort: Int,
            bootstrapPeer: Option[URI])(
      implicit
      system: ActorSystem,
      materializer: Materializer,
      executionContext: ExecutionContext): Behavior[String] = Behaviors.setup {

    context =>
      val nodeActor: ActorRef[NodeCommand] =
        context.spawn(
          new SimpleNode3(nodeName, serverHost, serverPort, bootstrapPeer).server,
          "NodeActor")

      val httpGatewayRoute: Route =
        HttpGateway.route(request => nodeActor ! Send(request.message))

      val startupListener: Behavior[Started] = Behaviors.receiveMessage {
        case Started(nodeUri) =>
          Http().bindAndHandle(route2HandlerFlow(httpGatewayRoute),
                               gatewayHost,
                               gatewayPort.toInt)

          Behavior.ignore
      }

      nodeActor ! Start(context.spawn(startupListener, "Node_Startup_Listener"))

      Behaviors.receiveMessage(_ => Behavior.ignore)
  }
}
