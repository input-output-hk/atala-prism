package io.iohk.cef.net.transport

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.net.SimpleNode2

object RLPxNode2 extends App {

  val config = ConfigFactory.parseString(
    """
      |akka {
      |  loglevel = "DEBUG"
      |  actor {
      |    debug {
      |      # enable function of LoggingReceive, which is to log any received message at
      |      # DEBUG level
      |      receive = on
      |    }
      |  }
      |}
    """.stripMargin)

  val start: Behavior[String] = Behaviors.setup {
    context =>
      val nodeA = new SimpleNode2("A", 3000, None)

      context.spawn(nodeA.server, "NodeA")
      val nodeB = new SimpleNode2("B", 3001, Some(nodeA.nodeUri))


      // start two nodes, one acting as a boostrap
          ???
      // when both started, send a message from A to B

      // and a message from B to A
  }



  ActorSystem(start, "main") ! "go"
}
