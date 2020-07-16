package io.iohk.node.services

import java.security.PublicKey
import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import io.iohk.cvp.crypto.ECSignature
import io.iohk.node.operations.ValidationError.InvalidValue
import io.iohk.node.operations._
import io.iohk.node.operations.path.Path
import io.iohk.prism.protos.{node_internal, node_models}
import org.slf4j.LoggerFactory

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

trait BlockProcessingService {

  def processBlock(
      block: node_internal.AtalaBlock,
      blockTimestamp: Instant,
      blockSequenceNumber: Int
  ): ConnectionIO[Boolean]
}

class BlockProcessingServiceImpl extends BlockProcessingService {

  // there are two implicit implementations for cats.Monad[doobie.free.connection.ConnectionIO],
  // one from doobie, the other for cats, making it ambiguous
  // we need to choose one
  implicit def _connectionIOMonad: cats.Monad[doobie.free.connection.ConnectionIO] =
    doobie.free.connection.AsyncConnectionIO

  protected val logger = LoggerFactory.getLogger(getClass)

  def parseOperation(
      signedOperation: node_models.SignedAtalaOperation,
      timestampInfo: TimestampInfo
  ): Either[ValidationError, Operation] = {
    signedOperation.getOperation.operation match {
      case _: node_models.AtalaOperation.Operation.CreateDid =>
        CreateDIDOperation.parse(signedOperation, timestampInfo)
      case _: node_models.AtalaOperation.Operation.UpdateDid =>
        UpdateDIDOperation.parse(signedOperation, timestampInfo)
      case _: node_models.AtalaOperation.Operation.IssueCredential =>
        IssueCredentialOperation.parse(signedOperation, timestampInfo)
      case _: node_models.AtalaOperation.Operation.RevokeCredential =>
        RevokeCredentialOperation.parse(signedOperation, timestampInfo)
      case op =>
        Left(InvalidValue(Path.root, op.getClass.getSimpleName, "Unknown operation"))
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
    * @return Left with underlying L type containing first error occured, Right with M[R] underlying type if there are no errors
    */
  private def eitherTraverse[A, L, R, M[X] <: TraversableOnce[X]](
      in: M[A]
  )(f: A => Either[L, R])(implicit cbf: CanBuildFrom[M[A], R, M[R]]): Either[L, M[R]] = {
    val builder = cbf(in)

    in.foldLeft(Either.right[L, builder.type](builder)) { (eitherBuilder, el) =>
        for {
          b <- eitherBuilder
          elResult <- f(el)
        } yield b.+=(elResult)
      }
      .map(_.result())
  }

  // ConnectionIO[Boolean] is a temporary type used to be able to unit tests this
  // it eventually will be replaced with ConnectionIO[Unit]
  override def processBlock(
      block: node_internal.AtalaBlock,
      blockTimestamp: Instant,
      blockSequenceNumber: Int
  ): ConnectionIO[Boolean] = {
    val operations = block.operations.toList
    val operationsWithSeqNumbers = operations.zipWithIndex
    val parsedOperationsEither = eitherTraverse(operationsWithSeqNumbers) {
      case (signedOperation, osn) =>
        parseOperation(signedOperation, TimestampInfo(blockTimestamp, blockSequenceNumber, osn)).left
          .map(err => (signedOperation, err))
    }

    parsedOperationsEither match {
      case Left((signedOperation, error)) =>
        logger.warn(
          s"Occurred invalid operation; ignoring whole block as invalid:\n${error.render}\nOperation:\n${signedOperation.toProtoString}"
        )
        connection.pure(false)
      case Right(parsedOperations) =>
        (parsedOperations zip operations)
          .traverse {
            case (parsedOperation, protoOperation) =>
              val result: ConnectionIO[Unit] = for {
                // we want operations to be atomic - either it is applied correctly or the state is not modified
                // we are using SQL savepoints for that, which can be used to do subtransactions
                savepoint <- connection.setSavepoint
                _ <- {
                  processOperation(parsedOperation, protoOperation)
                    .flatMap {
                      case Right(_) =>
                        logger.info(s"Operation applied:\n${parsedOperation.digest}")
                        connection.releaseSavepoint(savepoint)
                      case Left(err) =>
                        logger.warn(
                          s"Operation was not applied:\n${err.toString}\nOperation:\n${protoOperation.toProtoString}"
                        )
                        connection.rollback(savepoint)
                    }
                }
              } yield ()
              result
          }
          .map(_ => true)
    }
  }

  def processOperation(
      operation: Operation,
      protoOperation: node_models.SignedAtalaOperation
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
    } yield ()
    result.value
  }

  def verifySignature(key: PublicKey, protoOperation: node_models.SignedAtalaOperation): Either[StateError, Unit] = {
    try {
      Either.cond(
        ECSignature.verify(key, protoOperation.getOperation.toByteArray, protoOperation.signature.toByteArray.toVector),
        (),
        StateError.InvalidSignature()
      )
    } catch {
      case ex: java.security.SignatureException =>
        logger.warn("Unable to parse signature", ex)
        Left(StateError.InvalidSignature())
    }
  }
}
