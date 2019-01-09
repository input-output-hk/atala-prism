package io.iohk.cef.agreements

sealed trait AgreementMessage[T] {
  val correlationId: String
}

object AgreementsMessage {

  case class Propose[T](correlationId: String, proposedBy: UserId, data: T) extends AgreementMessage[T]

  case class Agree[T](correlationId: String, agreedBy: UserId, data: T) extends AgreementMessage[T]

  case class Decline[T](correlationId: String, declinedBy: UserId) extends AgreementMessage[T]

  def messageCata[T, R](fPropose: Propose[T] => R, fAgree: Agree[T] => R, fDecline: Decline[T] => R)(
    message: AgreementMessage[T]): R = message match {

    case p: Propose[T] =>
      fPropose(p)
    case a: Agree[T] =>
      fAgree(a)
    case d: Decline[T] =>
      fDecline(d)
  }
}
