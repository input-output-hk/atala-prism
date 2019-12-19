package io.iohk.node.operations

import java.security.MessageDigest
import java.time.LocalDate

import cats.data.EitherT
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.node.models.{CredentialId, DIDSuffix, SHA256Digest}
import io.iohk.node.operations.path._
import io.iohk.node.repositories.daos.CredentialsDAO
import io.iohk.node.repositories.daos.CredentialsDAO.CreateCredentialData
import io.iohk.nodenew.{geud_node_new => proto}

import scala.util.Try

case class IssueCredentialOperation(
    credentialId: CredentialId,
    issuer: DIDSuffix,
    contentHash: SHA256Digest,
    issuanceDate: LocalDate,
    digest: SHA256Digest
) extends Operation {
  override def getKey(keyId: String): Either[StateError, OperationKey] = Right(OperationKey.DeferredKey(issuer, keyId))

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] = EitherT {
    CredentialsDAO
      .insert(CreateCredentialData(credentialId, digest, issuer, contentHash, java.sql.Date.valueOf(issuanceDate)))
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          StateError.EntityExists("credential", credentialId.id): StateError
        case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
          // that shouldn't happen, as key verification requires issuer in the DB,
          // but puting it here just in the case
          StateError.EntityMissing("issuer", issuer.suffix)
      }
  }
}

object IssueCredentialOperation extends OperationCompanion[IssueCredentialOperation] {

  protected def parseDate(date: ValueAtPath[proto.Date]): Either[ValidationError, LocalDate] = {
    for {
      year <- date.child(_.year, "year").parse { year =>
        Either.cond(year > 0, year, "Year needs to be specified as positive value")
      }
      month <- date.child(_.month, "month").parse { month =>
        Either.cond(month >= 1 && month <= 12, month, "Month has to be specified and between 1 and 12")
      }
      parsedDate <- date.child(_.day, "day").parse { day =>
        Try(LocalDate.of(year, month, day)).toEither.left
          .map(_ => "Day has to be specified and a proper day in the month")
      }
    } yield parsedDate
  }

  override def parse(operation: proto.AtalaOperation): Either[ValidationError, IssueCredentialOperation] = {
    val operationDigest = SHA256Digest(MessageDigest.getInstance("SHA-256").digest(operation.toByteArray))
    val credentialId = CredentialId(operationDigest)
    val createOperation = ValueAtPath(operation, Path.root).child(_.getIssueCredential, "issueCredential")

    for {
      credentialData <- createOperation.childGet(_.credentialData, "credentialData")
      _ <- credentialData.child(_.id, "id").parse { id =>
        Either.cond(id.isEmpty, (), "Id must be empty for DID creation operation")
      }
      issuer <- credentialData.child(_.issuer, "issuer").parse { issuerDidSuffix =>
        Either.cond(
          DIDSuffix.DID_SUFFIX_RE.pattern.matcher(issuerDidSuffix).matches(),
          DIDSuffix(issuerDidSuffix),
          "must be a valid DID suffix"
        )
      }
      contestHash <- credentialData.child(_.contentHash, "contentHash").parse { contentHash =>
        Either.cond(
          contentHash.size == SHA256Digest.BYTE_LENGTH,
          SHA256Digest(contentHash.toByteArray),
          s"mush be of ${SHA256Digest.BYTE_LENGTH} bytes"
        )
      }
      issuanceDate <- credentialData.childGet(_.issuanceDate, "issuanceDate").flatMap(parseDate)
    } yield IssueCredentialOperation(credentialId, issuer, contestHash, issuanceDate, operationDigest)
  }
}
