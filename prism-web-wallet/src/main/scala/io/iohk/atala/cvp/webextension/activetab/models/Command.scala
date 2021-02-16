package io.iohk.atala.cvp.webextension.activetab.models

import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject}

import scala.util.Try

/**
  * Internal typed-message to request the extension isolated context to perform an operation.
  */
private[activetab] sealed trait Command extends Product with Serializable

private[activetab] object Command {

  final case object GetSdkDetails extends Command
  final case object GetWalletStatus extends Command
  final case object CreateSession extends Command

  final case class RequestSignature(sessionId: String, subject: CredentialSubject) extends Command

  final case class SignConnectorRequest(sessionId: String, request: ConnectorRequest) extends Command
  final case class VerifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String,
      encodedMerkleProof: String
  ) extends Command

  def decode(string: String): Try[Command] = {
    parse(string).toTry
      .flatMap(_.as[Command].toTry)
  }
}
