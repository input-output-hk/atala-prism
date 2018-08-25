package io.iohk.cef.utils

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Scheduler}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Manages concurrent access to state using akka mailboxes.
  */
class StateActor[T](initialState: T)(implicit val actorSystem: ActorSystem) {

  private sealed trait Request
  private case class Set(t: T, replyTo: ActorRef[Unit]) extends Request
  private case class Get(replyTo: ActorRef[T]) extends Request

  private val stateHolder = actorSystem.spawnAnonymous(pipe(initialState))
  private implicit val timeout: Timeout = 1 second
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  def get: Future[T] = {
    stateHolder ? (ref => Get(ref))
  }

  def set(t: T): Future[Unit] = {
    stateHolder ? (ref => Set(t, ref))
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
