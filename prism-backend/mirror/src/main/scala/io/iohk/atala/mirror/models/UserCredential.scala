package io.iohk.atala.mirror.models

import java.time.Instant

import io.circe.generic.auto._
import enumeratum.{DoobieEnum, Enum, EnumEntry}

import io.iohk.atala.prism.models.{ConnectionToken, ConnectorMessageId}
import io.iohk.atala.mirror.models.UserCredential.{
  CredentialStatus,
  MessageReceivedDate,
  RawCredential,
  UserCredentialException
}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.mirror.protos.ivms101.Person
import io.iohk.atala.prism.credentials.Credential

case class UserCredential(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: Option[DID],
    messageId: ConnectorMessageId,
    messageReceivedDate: MessageReceivedDate,
    status: CredentialStatus
) {

  def toPerson: Either[UserCredentialException, Option[Person]] = {
    for {
      credential <-
        Credential.fromString(rawCredential.rawCredential).left.map(error => UserCredentialException(error.getMessage))
      person <- credential.content.credentialType.left.map(error => UserCredentialException(error.getMessage)).flatMap {
        case KycCredential.KYC_CREDENTIAL_TYPE :: Nil =>
          KycCredential
            .fromCredentialContent(credential.content)
            .flatMap(_.toPerson)
            .map(Some.apply)
            .left
            .map(error => UserCredentialException(error.getMessage))
        case RedlandIdCredential.REDLAND_ID_CREDENTIAL_TYPE =>
          RedlandIdCredential
            .fromCredentialContent(credential.content)
            .flatMap(_.toPerson)
            .map(Some.apply)
            .left
            .map(error => UserCredentialException(error.getMessage))
        case _ =>
          // Here might be non-identity credentials added - we don't want to raise an error in such a case.
          Right(None)
      }
    } yield person
  }
}

object UserCredential {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class MessageReceivedDate(date: Instant) extends AnyVal

  sealed abstract class CredentialStatus(value: String) extends EnumEntry {
    override def entryName: String = value
  }

  object CredentialStatus extends Enum[CredentialStatus] with DoobieEnum[CredentialStatus] {
    lazy val values = findValues

    final case object Received extends CredentialStatus("RECEIVED")
    final case object Valid extends CredentialStatus("VALID")
    final case object Invalid extends CredentialStatus("INVALID")
    final case object Revoked extends CredentialStatus("REVOKED")
  }

  case class UserCredentialException(message: String) extends Exception(message)
}
