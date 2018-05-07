package io.iohk.cef.discovery

import akka.actor.ActorSystem
import akka.testkit.TestKit
import io.iohk.cef.test.StopAfterAll
import org.scalatest.WordSpecLike

class DiscoveryListenerSpec
  extends TestKit(ActorSystem("DiscoveryListenerSpec"))
    with WordSpecLike
    with StopAfterAll {


  "A DiscoveryListener" must {
    "receive UDP packets and forward them to the parent" in {
      pending
    }
    "send UDP packets" in {
      pending
    }
    "not bind to the UDP port at startup" in {
      pending
    }
  }
}
