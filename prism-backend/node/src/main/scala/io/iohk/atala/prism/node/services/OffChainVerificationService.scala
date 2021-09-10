package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.effect.IO
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.operations.parseOperationWithMockedLedger
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, OperationsVerificationRepository}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

class OffChainVerificationService(
    credentialBatchesRepository: CredentialBatchesRepository[IO],
    operationsVerificationRepository: OperationsVerificationRepository[IO]
) {
  implicit val operationsVerificationRepositoryImplicit: OperationsVerificationRepository[IO] =
    operationsVerificationRepository
  implicit val credentialBatchesRepositoryImplicit: CredentialBatchesRepository[IO] =
    credentialBatchesRepository

  def verifyOffChain(signedAtalaOperation: SignedAtalaOperation): EitherT[IO, NodeError, Unit] = {
    val operationE = parseOperationWithMockedLedger(signedAtalaOperation).left.map { validationError =>
      NodeError.InvalidDataError(validationError.explanation): NodeError
    }

    for {
      operation <- EitherT.fromEither[IO](operationE)
      _ <- operation.verifyOffChain(signedAtalaOperation.signedWith)
      _ <- operation.applyOffChain(signedAtalaOperation.signedWith)
    } yield ()
  }
}

object OffChainVerificationService {
  def apply(
      credentialBatchesRepository: CredentialBatchesRepository[IO],
      operationsVerificationRepository: OperationsVerificationRepository[IO]
  ): OffChainVerificationService =
    new OffChainVerificationService(credentialBatchesRepository, operationsVerificationRepository)
}
