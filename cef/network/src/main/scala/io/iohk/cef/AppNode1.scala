package io.iohk.cef

import akka.{actor => untyped}
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

object App extends AppBase {


  def main(args: Array[String]): Unit = {
    val system = createActor(8, Set(9), Capabilities(1))

    val spy = system.actorOf(LogEverything.props())

    system.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
