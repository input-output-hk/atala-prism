package io.iohk.cef.test
import io.iohk.network.{MessageStream, Network, NodeId}
import monix.execution.Scheduler
import monix.reactive.Observable

class DummyNoMessageNetwork[Message](implicit scheduler: Scheduler) extends Network[Message] {

  override def disseminateMessage(message: Message): Unit = ()

  override def messageStream: MessageStream[Message] = new DummyMessageStream(Observable.empty)

  override def sendMessage(nodeId: NodeId, message: Message): Unit = ()

}
