package io.iohk.cef

import io.iohk.cef.App.createActor
import io.iohk.cef.discovery.CompatibleNodeFound
import io.iohk.cef.network.Capabilities

object AppNode2 {


  def main(args: Array[String]): Unit = {
    val system = createActor(9, Set(8), Capabilities(1))

    val spy = system.actorOf(LogEverything.props())

    system.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
