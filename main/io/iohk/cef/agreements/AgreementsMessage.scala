package io.iohk.cef.agreements

sealed trait AgreementMessage[T]

object AgreementsMessage {

  case class Propose[T](correlationId: String, proposedBy: UserId, data: T) extends AgreementMessage[T]

  case class Agree[T](correlationId: String, agreedBy: UserId, data: T) extends AgreementMessage[T]

  case class Decline[T](correlationId: String, declinedBy: UserId) extends AgreementMessage[T]
}
