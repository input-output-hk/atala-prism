package io.iohk.cef.discovery

import akka.actor.ActorSystem
import akka.testkit.TestKit
import io.iohk.cef.test.StopAfterAll
import org.scalatest.FlatSpecLike

class DiscoveryListenerSpec
  extends TestKit(ActorSystem("DiscoveryListenerSpec"))
    with FlatSpecLike
    with StopAfterAll {


  "A DiscoveryListener" should "receive UDP packets" in {
    //val actor = system.actorOf(DiscoveryListener.props())
  }
}
