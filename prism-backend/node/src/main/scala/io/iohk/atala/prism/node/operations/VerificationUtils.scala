package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.repositories.OperationsVerificationRepository

object VerificationUtils {

  /**
    *
   * Checks that (signedWithDidSuffix, signedWithKeyId) key was not revoked before usage.
    */
  def notRevokedBefore(signedWithDidSuffix: DidSuffix, signedWithKeyId: String)(implicit
      operationsVerificationRepository: OperationsVerificationRepository[IO]
  ): EitherT[IO, errors.NodeError, Unit] =
    for {
      isRevoked <- EitherT {
        operationsVerificationRepository
          .isRevoked(signedWithDidSuffix, signedWithKeyId)
          .map(_.asRight[errors.NodeError])
      }
      _ <- EitherT.cond[IO](
        !isRevoked,
        (),
        errors.NodeError.KeyAlreadyRevoked(signedWithDidSuffix, signedWithKeyId): errors.NodeError
      )
    } yield ()

  def keyNotUsedBefore(signedWithDidSuffix: DidSuffix, signedWithKeyId: String)(implicit
      operationsVerificationRepository: OperationsVerificationRepository[IO]
  ): EitherT[IO, errors.NodeError, Unit] =
    for {
      isKeyUsed <- EitherT {
        operationsVerificationRepository
          .isKeyUsed(signedWithDidSuffix, signedWithKeyId)
          .map(_.asRight[errors.NodeError])
      }
      _ <- EitherT.cond[IO](
        !isKeyUsed,
        (),
        errors.NodeError.KeyUsedBeforeAddition(signedWithDidSuffix, signedWithKeyId): errors.NodeError
      )
    } yield ()

  def didNotUsedBefore(didSuffix: DidSuffix)(implicit
      operationsVerificationRepository: OperationsVerificationRepository[IO]
  ): EitherT[IO, errors.NodeError, Unit] =
    for {
      isDidUsed <- EitherT {
        operationsVerificationRepository
          .isDidUsed(didSuffix)
          .map(_.asRight[errors.NodeError])
      }
      _ <- EitherT.cond[IO](
        !isDidUsed,
        (),
        errors.NodeError.DidUsedBeforeCreation(didSuffix): errors.NodeError
      )
    } yield ()

  def checkPreviousOperationDuplication(
      previousOperation: Sha256Digest
  )(implicit
      operationsVerificationRepository: OperationsVerificationRepository[IO]
  ): EitherT[IO, errors.NodeError, Unit] =
    EitherT {
      operationsVerificationRepository
        .previousOperationExists(previousOperation)
        .map(exists => Either.cond(!exists, (), errors.NodeError.PreviousOperationDuplication(previousOperation)))
    }
}
