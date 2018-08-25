package io.iohk.cef.utils

import akka.actor.typed.scaladsl.Behaviors.{receive, unhandled}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same}
import akka.actor.typed.scaladsl.StashBuffer
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Scheduler}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Manages concurrent access to state using akka mailboxes.
  */
class StateActor[T](implicit actorSystem: ActorSystem) {
  sealed trait Request
  private case class Set(t: T, replyTo: ActorRef[Unit]) extends Request
  private case class Get(replyTo: ActorRef[T]) extends Request

  private val stateHolder = actorSystem.spawnAnonymous(pipe())
  private implicit val timeout: Timeout = 10 millis
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  val buffer = StashBuffer[Request](10)

  def get: Future[T] = {
    implicit val ex: ExecutionContext = actorSystem.dispatcher
    (stateHolder ? (ref => Get(ref))) recover {
      case cause => throw new IllegalStateException("The actor must be initialized with Set.")
    }
  }

  def set(t: T): Future[Unit] = {
    stateHolder ? (ref => Set(t, ref))
  }

  private def pipe(): Behavior[Request] = receive { (context, message) =>
    message match {
      case Set(t, replyTo) =>
        replyTo ! Unit
        buffer.unstashAll(context, pipe(t))
      case request @ Get(replyTo) =>
        buffer.stash(request)
        unhandled
    }
  }

  private def pipe(state: T): Behavior[Request] = receiveMessage {
    case Set(t, replyTo) =>
      replyTo ! Unit
      pipe(t)
    case Get(replyTo) =>
      replyTo ! state
      same
  }
}
