package io.iohk.cef.demo

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.{actor => untyped}
import io.iohk.cef.discovery.DiscoveryManager.{DiscoveredNodes, GetDiscoveredNodes}
import io.iohk.cef.discovery._
import io.iohk.cef.network.Capabilities

class LogEverything extends untyped.Actor with untyped.ActorLogging {

  override def receive: Receive = {
    case m =>
      println(s"Message Received: $m")
  }
}

object LogEverything {
  def props() = untyped.Props(new LogEverything)
}

object AppNode1 extends AppBase {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("cef_system")

    val actor = createActor(8, Set(9), Capabilities(1))

    val spy = actorSystem.actorOf(LogEverything.props())

    actorSystem.eventStream.subscribe(spy, classOf[CompatibleNodeFound])

    val printer: ActorRef[DiscoveredNodes] =
      actorSystem.spawn(Behaviors.receiveMessage[DiscoveredNodes](printMessage), "printer")

    actor ! GetDiscoveredNodes(printer)
  }

  private def printMessage[M](message: M): Behavior[M] = {
    println(message)
    Behavior.same
  }
}
