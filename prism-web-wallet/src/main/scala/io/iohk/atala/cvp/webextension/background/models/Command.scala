package io.iohk.atala.cvp.webextension.background.models

import cats.data.ValidatedNel
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.cvp.webextension.background.wallet.{SigningRequest, WalletStatus}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models._

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
  final case class SignedConnectorResponse(signedMessage: SignedMessage)

  final case class VerifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String,
      encodedMerkleProof: String
  ) extends CommandWithResponse[VerifySignedCredentialResponse]
  final case class VerifySignedCredentialResponse(result: ValidatedNel[VerificationError, Unit])

  final case class SignatureResult(signature: String)

  final case object GetSigningRequests extends CommandWithResponse[SigningRequests]
  final case class SigningRequests(requests: List[SigningRequest])

  final case class SignRequest(requestId: Int) extends CommandWithResponse[Unit]
  final case class RejectRequest(requestId: Int) extends CommandWithResponse[Unit]

  final case object GetWalletStatus extends CommandWithResponse[WalletStatusResult];
  final case class WalletStatusResult(status: WalletStatus)

  final case class TransactionInfo(id: String)
  final case object GetTransactionInfo extends CommandWithResponse[TransactionInfo];

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
