package io.iohk.atala.cvp.webextension.background.models

import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.background.wallet.SigningRequest

import scala.util.Try

/**
  * Internal typed-message to request the background context to perform an operation.
  */
private[background] sealed trait Command extends Product with Serializable
private[background] sealed trait CommandWithResponse[Resp] extends Command

private[background] object Command {

  final case class SendBrowserNotification(title: String, message: String) extends CommandWithResponse[Event]

  final case class RequestSignature(message: String) extends CommandWithResponse[SignatureResult]
  final case class SignatureResult(signature: String)

  final case class CreateKey(keyName: String) extends CommandWithResponse[Unit]

  final case object ListKeys extends CommandWithResponse[KeyList]
  final case class KeyList(names: List[String])

  final case object GetSigningRequests extends CommandWithResponse[SigningRequests]
  final case class SigningRequests(requests: List[SigningRequest])

  final case class SignRequestWithKey(requestId: Int, keyName: String) extends CommandWithResponse[Unit]

  final case class UnlockWallet(password: String) extends CommandWithResponse[Unit]
  final case class LockWallet() extends CommandWithResponse[Unit]

  def decode(string: String): Try[Command] = {
    parse(string).toTry
      .flatMap(_.as[Command].toTry)
  }
}
