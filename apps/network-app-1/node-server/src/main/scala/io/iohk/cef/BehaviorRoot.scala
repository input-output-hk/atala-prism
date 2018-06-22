package io.iohk.cef

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult._
import akka.stream.Materializer
import io.iohk.cef.demo.SimpleNode3
import io.iohk.cef.demo.SimpleNode3._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Promise}

object BehaviorRoot {

  def start(node: SimpleNode3,
            gatewayHost: String,
            gatewayPort: Int)(
    implicit
    system: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext): Behavior[String] = Behaviors.setup {

    context =>

      val nodeActor: ActorRef[NodeCommand] =
        context.spawn(node.server, "NodeActor")

      def serverListener(currentRequests: mutable.Map[String, Promise[Int]]): Behavior[NodeResponse] =

        Behaviors.receiveMessage {

          case Started(_) => // p2p server has started. boot the http server

            val httpGatewayRoute: Route =
              HttpGateway.route(request => {
                val message = request.message
                context.log.info(s"Gateway received: $message")
                val promise = Promise[Int]()

                currentRequests.put(message, promise)

                nodeActor ! Send(message)

                promise.future
              })

            Http().bindAndHandle(route2HandlerFlow(httpGatewayRoute),
              gatewayHost,
              gatewayPort.toInt)

            Behavior.same

          case Confirmed(msg, peerCount) =>
            context.log.info(s"Confirming message $msg")
            currentRequests.remove(msg).foreach(_.success(peerCount))

            Behavior.same
        }

      nodeActor ! Start(context.spawn(serverListener(new ConcurrentHashMap[String, Promise[Int]]().asScala), "Node_Startup_Listener"))

      Behaviors.receiveMessage(_ => Behavior.ignore)
  }
}
