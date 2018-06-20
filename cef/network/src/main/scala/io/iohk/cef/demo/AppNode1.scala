package io.iohk.cef.demo

import akka.{actor => untyped}
import io.iohk.cef.network.Capabilities

object AppNode1 extends AppBase {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: untyped.ActorSystem = untyped.ActorSystem("cef_system")

    createActor(8, Set(9), Capabilities(1))
  }
}
