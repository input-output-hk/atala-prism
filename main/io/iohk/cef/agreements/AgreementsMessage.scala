package io.iohk.cef.agreements

import java.util.UUID

sealed trait AgreementMessage[T] {
  val correlationId: UUID
}

object AgreementsMessage {

  case class Propose[T](correlationId: UUID, proposedBy: UserId, data: T) extends AgreementMessage[T]

  case class Agree[T](correlationId: UUID, agreedBy: UserId, data: T) extends AgreementMessage[T]

  case class Decline[T](correlationId: UUID, declinedBy: UserId) extends AgreementMessage[T]

  def catamorphism[T, R](fPropose: Propose[T] => R, fAgree: Agree[T] => R, fDecline: Decline[T] => R)(
      message: AgreementMessage[T]
  ): R = message match {

    case p: Propose[T] =>
      fPropose(p)
    case a: Agree[T] =>
      fAgree(a)
    case d: Decline[T] =>
      fDecline(d)
  }
}
