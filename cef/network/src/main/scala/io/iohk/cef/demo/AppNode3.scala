package io.iohk.cef.demo

import akka.{actor => untyped}
import io.iohk.cef.demo.AppNode1.createActor
import io.iohk.cef.network.Capabilities

object AppNode3 {
  def main(args: Array[String]): Unit = {

    implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("cef_system")

    createActor(10, Set(8), Capabilities(1))
  }
}
