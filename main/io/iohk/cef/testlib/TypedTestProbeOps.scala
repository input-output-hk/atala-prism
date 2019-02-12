package io.iohk.cef.test

import akka.actor.testkit.typed.scaladsl.TestProbe
import org.scalatest.Matchers._

import scala.reflect.ClassTag

object TypedTestProbeOps {

  implicit class TypedTestProbeOps[T: ClassTag](testProbe: TestProbe[T]) {
    def uponReceivingMessage[U](message: T, f: T => U): U =
      f(testProbe.expectMessageType[T])

    def uponReceivingMessage[U](f: PartialFunction[T, U]): U = {
      val message = testProbe.expectMessageType[T]
      if (f.isDefinedAt(message))
        f(message)
      else
        fail(s"Message $message is not defined at a required partial function.")
    }

    def uponReceivingMessage[U](messagePredicate: T => Boolean, f: T => U): U = {
      val message = testProbe.expectMessageType[T]
      if (messagePredicate(message))
        f(message)
      else
        fail(s"Message $message does not satisfy a required predicate.")
    }
  }
}
