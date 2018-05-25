package io.iohk.cef.demo

import io.iohk.cef.db.ConnectionPool
import io.iohk.cef.demo.AppNode1.createActor
import io.iohk.cef.discovery.CompatibleNodeFound
import io.iohk.cef.network.Capabilities

object AppNode2 {


  def main(args: Array[String]): Unit = {
    val pool = new ConnectionPool("default2")

    val (system, actor) = createActor(9, Set(8), Capabilities(1), pool)

    val spy = system.actorOf(LogEverything.props())

    system.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
