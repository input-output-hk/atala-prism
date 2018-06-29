package io.iohk.cef.ledger.identity

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import io.iohk.cef.ledger.Ledger

import scala.concurrent.Future

trait IdentityLedger extends Ledger {

  type Identity
  type PublicKey

  val storage: IdentityLedgerStorage

  trait IdentityLedgerRequest

  case class Claim(identity: Identity, key: PublicKey, replyTo: ActorRef[IdentityLedgerResponse]) extends IdentityLedgerRequest
  case class Link(identity: Identity, key: PublicKey, replyTo: ActorRef[IdentityLedgerResponse]) extends IdentityLedgerRequest
  case class Unlink(identity: Identity, key: PublicKey, replyTo: ActorRef[IdentityLedgerResponse]) extends IdentityLedgerRequest
  case class isLinked(identity: Identity, key: PublicKey, replyTo: ActorRef[IdentityLedgerResponse]) extends IdentityLedgerRequest

  trait IdentityLedgerResponse

  case class Linked(identity: Identity, key: PublicKey) extends IdentityLedgerResponse
  case class Unlinked(identity: Identity, key: PublicKey) extends IdentityLedgerResponse
  case class LinkInfo(identity: Identity, key: PublicKey) extends IdentityLedgerResponse
  case class Error(exception: Throwable) extends IdentityLedgerResponse

  trait IdentityLedgerStorage {
    def claim(identity: Identity, key: PublicKey): Future[Unit]
    def link(identity: Identity, newKey: PublicKey): Future[Unit]
    def unlink(identity: Identity, key: PublicKey): Future[Unit]
    def isLinked(identity: Identity, key: PublicKey): Future[Boolean]
  }
}

object IdentityLedger {

  def behavior(identityLedger: IdentityLedger) =
    Behaviors.setup[identityLedger.IdentityLedgerRequest] { _ =>

    import identityLedger._
    import io.iohk.cef.utils.PipeTypedSupport._

    def errorCreator(t: Throwable): IdentityLedgerResponse = Error(t)

    Behaviors.receiveMessage[IdentityLedgerRequest] {
      case Claim(identity, key, replyTo) =>
        storage.claim(identity, key)
          .map[IdentityLedgerResponse](_ => Linked(identity, key))
          .pipeTo(replyTo, errorCreator _)
        Behaviors.same
      case Link(identity, key, replyTo) =>
        storage.link(identity, key)
          .map[IdentityLedgerResponse](_ => Linked(identity, key))
          .pipeTo(replyTo, errorCreator _)
        Behaviors.same
      case Unlink(identity, key, replyTo) =>
        storage.unlink(identity, key)
          .map[IdentityLedgerResponse](_ => Unlinked(identity, key))
          .pipeTo(replyTo, errorCreator _)
        Behaviors.same
      case isLinked(identity, key, replyTo) =>
        storage.isLinked(identity, key)
          .map[IdentityLedgerResponse](_ => LinkInfo(identity, key))
          .pipeTo(replyTo, errorCreator _)
        Behaviors.same
    }
  }
}
