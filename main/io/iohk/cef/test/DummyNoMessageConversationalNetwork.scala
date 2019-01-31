package io.iohk.cef.test

import io.iohk.codecs.nio._
import io.iohk.network._
import io.iohk.network.discovery.NetworkDiscovery
import io.iohk.network.transport.Transports
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalatest.mockito.MockitoSugar._

import scala.reflect.runtime.universe._

class DummyNoMessageConversationalNetwork[Message: NioCodec: TypeTag](implicit scheduler: Scheduler)
    extends ConversationalNetwork[Message](mock[NetworkDiscovery], mock[Transports]) {

  override def messageStream: MessageStream[Message] = new DummyMessageStream(Observable.empty)

  override def sendMessage(nodeId: NodeId, message: Message): Unit = ()

  override val peerConfig: PeerConfig = PeerConfig(NodeId("1111"), TransportConfig(None))

}
