package io.iohk.cef

import akka.pattern.ask
import akka.{actor => untyped}
import io.iohk.cef.db.ConnectionPool
import io.iohk.cef.discovery._
import io.iohk.cef.network.Capabilities

import scala.concurrent.Await
import scala.concurrent.duration._

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
    val pool = new ConnectionPool("default")

    val (system, actor) = createActor(8, Set(9), Capabilities(1), pool)

    val spy = system.actorOf(LogEverything.props())

    system.eventStream.subscribe(spy, classOf[CompatibleNodeFound])

    implicit val timeout = akka.util.Timeout.durationToTimeout(10.minutes)

    Thread.sleep(10000)

    println(Await.result(actor ? GetDiscoveredNodes(), 5.seconds))
  }
}
