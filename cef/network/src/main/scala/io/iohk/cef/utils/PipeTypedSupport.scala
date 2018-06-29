package io.iohk.cef.utils

import akka.actor.typed.ActorRef

import scala.concurrent.Future
import scala.util.{Failure, Success}

object PipeTypedSupport {
  implicit class PipeableFuture[T](future: Future[T]) {
    def pipeTo[U](successRecipient: ActorRef[T], failureRecipient: ActorRef[U], t: Throwable => U) = {
      future.andThen{
        case Success(s) => successRecipient ! s
        case Failure(f) => failureRecipient ! f(t)
      }
    }

    def pipeTo(successRecipient: ActorRef[T], failureRecipient: ActorRef[Throwable]) = {
      pipeTo(successRecipient, failureRecipient, x => x)
    }

    def pipeTo(recipient: ActorRef[T], t: Throwable => T) =
      pipeTo(recipient, recipient, t)
  }
}
