package io.iohk.node.operations

import java.time.LocalDate

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.node.models.{CredentialId, DIDPublicKey, DIDSuffix, SHA256Digest}
import io.iohk.node.operations.path._
import io.iohk.node.repositories.daos.{CredentialsDAO, PublicKeysDAO}
import io.iohk.nodenew.{geud_node_new => proto}

case class RevokeCredentialOperation(
    credentialId: CredentialId,
    revocationDate: LocalDate,
    previousOperation: SHA256Digest,
    digest: SHA256Digest
) extends Operation {
  override def linkedPreviousOperation: Option[SHA256Digest] = Some(previousOperation)

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      issuerPrevOp <- EitherT[ConnectionIO, StateError, (DIDSuffix, SHA256Digest)] {
        CredentialsDAO
          .find(credentialId)
          .map(
            _.map(cred => (cred.issuer, cred.lastOperation))
              .toRight(StateError.EntityMissing("credential", credentialId.id))
          )
      }
      (issuer, prevOp) = issuerPrevOp
      key <- EitherT[ConnectionIO, StateError, DIDPublicKey] {
        PublicKeysDAO.find(issuer, keyId).map(_.toRight(StateError.UnknownKey(issuer, keyId)))
      }.subflatMap { didKey =>
        Either.cond(didKey.keyUsage.canIssue, didKey.key, StateError.InvalidSignature())
      }
    } yield CorrectnessData(key, Some(prevOp))
  }

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] = EitherT[ConnectionIO, StateError, Unit] {
    CredentialsDAO
      .revoke(credentialId, revocationDate)
      .map(_ => Right(()))
  }
}

object RevokeCredentialOperation extends OperationCompanion[RevokeCredentialOperation] {

  import ParsingUtils._

  override def parse(operation: proto.AtalaOperation): Either[ValidationError, RevokeCredentialOperation] = {

    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val revokeOperation = ValueAtPath(operation, Path.root).child(_.getRevokeCredential, "revokeCredential")

    for {
      credentialId <- revokeOperation.child(_.credentialId, "credentialId").parse { credentialId =>
        Either.cond(
          CredentialId.CREDENTIAL_ID_RE.pattern.matcher(credentialId).matches(),
          CredentialId(credentialId),
          "must follow valid format"
        )
      }
      previousOperation <- revokeOperation.child(_.previousOperationHash, "previousOperationHash").parse { hash =>
        Either.cond(
          hash.size() == SHA256Digest.BYTE_LENGTH,
          SHA256Digest(hash.toByteArray),
          s"mush have ${SHA256Digest.BYTE_LENGTH} bytes"
        )
      }
      revocationDate <- revokeOperation.childGet(_.revocationDate, "revocationDate").flatMap(parseDate)
    } yield RevokeCredentialOperation(credentialId, revocationDate, previousOperation, operationDigest)
  }
}
