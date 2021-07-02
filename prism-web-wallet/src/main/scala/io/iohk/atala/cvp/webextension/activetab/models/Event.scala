package io.iohk.atala.cvp.webextension.activetab.models

import cats.data.ValidatedNel
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.models.{SignedMessage, UserDetails}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.VerificationException

import scala.util.Try

/**
  * Internal typed-message used by the isolated extension context to reply to an operation
  */
private[activetab] sealed trait Event extends Product with Serializable

private[activetab] object Event {

  // TODO: Find a better way, possible returning something like Either[CommandRejected, Event]
  final case class CommandRejected(reason: String) extends Event
  final case class GotSdkDetails(extensionId: String) extends Event
  final case class GotWalletStatus(status: String) extends Event
  final case class GotUserSession(userDetails: UserDetails) extends Event
  final case class GotEnqueueRequestApprovalResultTransactionId(transactionId: String) extends Event
  final case class GotSignedResponse(signedMessage: SignedMessage) extends Event
  final case class SignedCredentialVerified(result: ValidatedNel[VerificationException, Unit]) extends Event

  def decode(string: String): Try[Event] = {
    parse(string).toTry
      .flatMap(_.as[Event].toTry)
  }
}
