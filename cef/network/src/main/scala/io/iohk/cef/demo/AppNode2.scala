package io.iohk.cef.demo

import akka.{actor => untyped}
import io.iohk.cef.demo.AppNode1.createActor
import io.iohk.cef.discovery.CompatibleNodeFound
import io.iohk.cef.network.Capabilities

object AppNode2 {
  def main(args: Array[String]): Unit = {

    implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("cef_system")

    createActor(9, Set(8), Capabilities(1))

    val spy = actorSystem.actorOf(LogEverything.props())

    actorSystem.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
