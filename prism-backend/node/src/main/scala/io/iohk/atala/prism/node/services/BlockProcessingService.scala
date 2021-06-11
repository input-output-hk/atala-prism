package io.iohk.atala.prism.node.services

import java.time.Instant
import cats.data.EitherT
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, ECSignature}
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.{AtalaOperationId, AtalaOperationStatus}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.ValidationError.InvalidValue
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.operations.path.Path
import io.iohk.atala.prism.node.repositories.daos.AtalaOperationsDAO
import io.iohk.atala.prism.protos.{node_internal, node_models}
import org.slf4j.LoggerFactory

import scala.collection.BuildFrom

trait BlockProcessingService {

  def processBlock(
      block: node_internal.AtalaBlock,
      transactionId: TransactionId,
      ledger: Ledger,
      blockTimestamp: Instant,
      blockIndex: Int
  ): ConnectionIO[Boolean]
}

class BlockProcessingServiceImpl extends BlockProcessingService {

  // there are two implicit implementations for cats.Monad[doobie.free.connection.ConnectionIO],
  // one from doobie, the other for cats, making it ambiguous
  // we need to choose one
  implicit def _connectionIOMonad: cats.Monad[doobie.free.connection.ConnectionIO] =
    doobie.free.connection.AsyncConnectionIO

  private val logger = LoggerFactory.getLogger(getClass)

  // ConnectionIO[Boolean] is a temporary type used to be able to unit tests this
  // it eventually will be replaced with ConnectionIO[Unit]
  override def processBlock(
      block: node_internal.AtalaBlock,
      transactionId: TransactionId,
      ledger: Ledger,
      blockTimestamp: Instant,
      blockIndex: Int
  ): ConnectionIO[Boolean] = {
    val operations = block.operations.toList
    val operationsWithSeqNumbers = operations.zipWithIndex
    val parsedOperationsEither = eitherTraverse(operationsWithSeqNumbers) {
      case (signedOperation, osn) =>
        parseOperation(
          signedOperation,
          LedgerData(transactionId, ledger, TimestampInfo(blockTimestamp, blockIndex, osn))
        ).left
          .map(err => (signedOperation, err))
    }

    parsedOperationsEither match {
      case Left((signedOperation, error)) =>
        logger.warn(
          s"Occurred invalid operation; ignoring whole block as invalid:\n${error.render}\nOperation:\n${signedOperation.toProtoString}"
        )
        AtalaOperationsDAO
          .updateAtalaOperationStatusBatch(
            operations.map(AtalaOperationId.of),
            AtalaOperationStatus.REJECTED
          )
          .as(false)
      case Right(parsedOperations) =>
        (parsedOperations zip operations)
          .traverse {
            case (parsedOperation, protoOperation) =>
              val atalaOperationId = AtalaOperationId.of(protoOperation)
              val result: ConnectionIO[Unit] = for {
                // we want operations to be atomic - either it is applied correctly or the state is not modified
                // we are using SQL savepoints for that, which can be used to do subtransactions
                savepoint <- connection.setSavepoint
                _ <- processOperation(parsedOperation, protoOperation, atalaOperationId)
                  .flatMap {
                    case Right(_) =>
                      logger.info(s"Operation applied:\n${parsedOperation.digest}")
                      connection.releaseSavepoint(savepoint)
                    case Left(err) =>
                      logger.warn(
                        s"Operation was not applied:\n${err.toString}\nOperation:\n${protoOperation.toProtoString}"
                      )
                      connection.rollback(savepoint)
                      AtalaOperationsDAO.updateAtalaOperationStatus(atalaOperationId, AtalaOperationStatus.REJECTED)
                  }
              } yield ()
              result
          }
          .as(true)
    }
  }

  def parseOperation(
      signedOperation: node_models.SignedAtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, Operation] = {
    signedOperation.getOperation.operation match {
      case _: node_models.AtalaOperation.Operation.CreateDid =>
        CreateDIDOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.UpdateDid =>
        UpdateDIDOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.IssueCredentialBatch =>
        IssueCredentialBatchOperation.parse(signedOperation, ledgerData)
      case _: node_models.AtalaOperation.Operation.RevokeCredentials =>
        RevokeCredentialsOperation.parse(signedOperation, ledgerData)
      case empty @ node_models.AtalaOperation.Operation.Empty =>
        Left(InvalidValue(Path.root, empty.getClass.getSimpleName, "Empty operation"))
    }
  }

  def processOperation(
      operation: Operation,
      protoOperation: node_models.SignedAtalaOperation,
      atalaOperationId: AtalaOperationId
  ): ConnectionIO[Either[StateError, Unit]] = {
    val result = for {
      correctnessData <- operation.getCorrectnessData(protoOperation.signedWith)
      CorrectnessData(key, previousOperation) = correctnessData
      _ <- EitherT.cond[ConnectionIO](
        operation.linkedPreviousOperation == previousOperation,
        (),
        StateError.InvalidPreviousOperation(): StateError
      )
      _ <- EitherT.fromEither[ConnectionIO](verifySignature(key, protoOperation))
      _ <- operation.applyState()
      _ <- EitherT.right[StateError](
        AtalaOperationsDAO.updateAtalaOperationStatus(atalaOperationId, AtalaOperationStatus.APPLIED)
      )
    } yield ()
    result.value
  }

  def verifySignature(key: ECPublicKey, protoOperation: node_models.SignedAtalaOperation): Either[StateError, Unit] = {
    try {
      Either.cond(
        EC.verify(protoOperation.getOperation.toByteArray, key, ECSignature(protoOperation.signature.toByteArray)),
        (),
        StateError.InvalidSignature()
      )
    } catch {
      case ex: java.security.SignatureException =>
        logger.warn("Unable to parse signature", ex)
        Left(StateError.InvalidSignature())
    }
  }

  /** Applies function to all sequence elements, up to the point error occurs
    *
   * @param in input sequence
    * @param f function to be applied to elements of the sequence
    * @tparam A type of input sequence element
    * @tparam L left alternative of Either returned by f
    * @tparam R right alternative of Either returned by f
    * @tparam M the input sequence type
    * @return Left with underlying L type containing first error occurred, Right with M[R] underlying type if there are no errors
    */
  private def eitherTraverse[A, L, R, M[X] <: IterableOnce[X]](
      in: M[A]
  )(f: A => Either[L, R])(implicit cbf: BuildFrom[M[A], R, M[R]]): Either[L, M[R]] = {
    val builder = cbf.newBuilder(in)

    in.iterator
      .foldLeft(Either.right[L, builder.type](builder)) { (eitherBuilder, el) =>
        for {
          b <- eitherBuilder
          elResult <- f(el)
        } yield b.+=(elResult)
      }
      .map(_.result())
  }
}
