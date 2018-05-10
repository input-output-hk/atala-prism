package io.iohk.cef.net.dummy

import akka.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, TestProbe}
import io.iohk.cef.net.dummy.DummyTransportProtocol.{DummyAddress, DummyPeerInfo}
import io.iohk.cef.net.transport.TransportProtocol._
import org.scalatest.{BeforeAndAfterAll, FunSpec}

class DummyTransportProtocolSpec extends FunSpec with ActorTestKit with BeforeAndAfterAll {

  val dummyTransportProtocol = new DummyTransportProtocol()
  val testKit = BehaviorTestKit(dummyTransportProtocol.createTransport())
  val probe: TestProbe[ConnectionReply[DummyPeerInfo]] = TestProbe()

  describe("Dummy transport protocol") {
    it("should create a connection to a local address") {
      testKit.run(Connect(DummyAddress("localhost"), probe.ref))
      probe.expectMessage(Connected(DummyPeerInfo("DummyAddress(localhost)")))
    }

    it("should raise an error when connecting to remote hosts") {
      testKit.run(Connect(DummyAddress("remotehost"), probe.ref))
      probe.expectMessage(ConnectionError[DummyPeerInfo]("Cannot connect to remote addresses. I'm a dummy."))
    }
  }

  override def afterAll(): Unit =
    shutdownTestKit()
}
