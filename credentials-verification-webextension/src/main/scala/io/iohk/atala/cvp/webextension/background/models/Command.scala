package io.iohk.atala.cvp.webextension.background.models

import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.background.wallet.{Role, SigningRequest, WalletStatus}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject, SignedMessage, UserDetails}

import scala.util.Try

/**
  * Internal typed-message to request the background context to perform an operation.
  */
private[background] sealed trait Command extends Product with Serializable
private[background] sealed trait CommandWithResponse[Resp] extends Command

private[background] object Command {

  final case class SendBrowserNotification(title: String, message: String) extends CommandWithResponse[Event]

  final case class RequestSignature(sessionId: String, subject: CredentialSubject) extends CommandWithResponse[Unit]

  final case class SignConnectorRequest(sessionId: String, request: ConnectorRequest)
      extends CommandWithResponse[SignedConnectorResponse]

  final case class SignedConnectorResponse(signedMessage: SignedMessage) extends CommandWithResponse[Event]

  final case class SignatureResult(signature: String)

  final case class CreateKey(keyName: String) extends CommandWithResponse[Unit]

  final case object ListKeys extends CommandWithResponse[KeyList]
  final case class KeyList(names: List[String])

  final case object GetSigningRequests extends CommandWithResponse[SigningRequests]
  final case class SigningRequests(requests: List[SigningRequest])

  final case class SignRequest(requestId: Int) extends CommandWithResponse[Unit]

  final case object GetWalletStatus extends CommandWithResponse[WalletStatusResult];
  final case class WalletStatusResult(status: WalletStatus)

  final case object GetUserSession extends CommandWithResponse[UserDetails];

  final case class CreateWallet(
      password: String,
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ) extends CommandWithResponse[Unit]

  final case class UnlockWallet(password: String) extends CommandWithResponse[Unit]
  final case class LockWallet() extends CommandWithResponse[Unit]
  final case class RecoverWallet(password: String, mnemonic: Mnemonic) extends CommandWithResponse[Unit]

  def decode(string: String): Try[Command] = {
    parse(string).toTry
      .flatMap(_.as[Command].toTry)
  }
}
