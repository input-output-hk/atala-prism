package io.iohk.cef.test

import akka.actor.typed.{ActorContext, Behavior, ExtensibleBehavior, Signal}

import scala.collection.mutable
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._

class MockBehavior[T] extends ExtensibleBehavior[T] {

  private val receivedMessages = mutable.ListBuffer[T]()

  def expectMessage(message: T): Unit = {
    eventually { receivedMessages should contain (message) }
  }

  override def receive(ctx: ActorContext[T], msg: T): Behavior[T] = {
    receivedMessages.append(msg)
    Behavior.same
  }

  override def receiveSignal(ctx: ActorContext[T], msg: Signal): Behavior[T] =
    Behavior.same
}

object MockBehavior {
  def mockBehavior[T]: MockBehavior[T] = new MockBehavior[T]()
}
