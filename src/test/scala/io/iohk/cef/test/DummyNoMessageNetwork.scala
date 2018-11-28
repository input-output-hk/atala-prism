package io.iohk.cef.test
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable

class DummyNoMessageNetwork[Message] extends Network[Message] {

  override def disseminateMessage(message: Message): Unit = ()

  override def messageStream: MessageStream[Message] = new DummyMessageStream(Observable.empty, TestScheduler())

  override def sendMessage(nodeId: NodeId, message: Message): Unit = ()

}
