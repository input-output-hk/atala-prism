package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpECDSA
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.node.metrics.OperationsCounters
import io.iohk.atala.prism.node.models.AtalaOperationId
import io.iohk.atala.prism.node.models.AtalaOperationStatus
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.Ledger
import io.iohk.atala.prism.node.models.TimestampInfo
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.daos.AtalaOperationsDAO
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.util.chaining._
import scala.util.control.NonFatal

case class BlockProcessingInfo(
    block: node_models.AtalaBlock,
    transactionId: TransactionId,
    ledger: Ledger,
    blockTimestamp: Instant,
    blockIndex: Int
)
// This service syncs Node state with the underlying ledger
trait BlockProcessingService {

  // Iterates over transactions in the Cardano block, retrieves operations from transaction metadata,
  // applies every operation to the Node state (e.g. update DID Document stored in the database)
  def processBlock(
      block: node_models.AtalaBlock,
      transactionId: TransactionId,
      ledger: Ledger,
      blockTimestamp: Instant,
      blockIndex: Int
  ): ConnectionIO[Boolean]

  // Process a batch of blocks in a single transaction
  def processBlockBatch(
      blocks: List[BlockProcessingInfo]
  ): ConnectionIO[Boolean]
}

class BlockProcessingServiceImpl(applyOperationConfig: ApplyOperationConfig) extends BlockProcessingService {
  private val logger = LoggerFactory.getLogger(getClass)

  override def processBlockBatch(
      blocks: List[BlockProcessingInfo]
  ): ConnectionIO[Boolean] = {
    val methodName = "processBlockBatch"
    val (allInvalid, allValid) = blocks
      .flatMap { blockInfo =>
        val operations = blockInfo.block.operations.toList
        val operationsWithSeqNumbers = operations.zipWithIndex
        operationsWithSeqNumbers.map { case (signedOperation, index) =>
          parseOperation(
            signedOperation,
            LedgerData(
              blockInfo.transactionId,
              blockInfo.ledger,
              TimestampInfo(blockInfo.blockTimestamp.toEpochMilli, blockInfo.blockIndex, index)
            )
          ) match {
            case Left(err) => Left((blockInfo, signedOperation, err))
            case Right(parsed) => Right((blockInfo, signedOperation, parsed))
          }
        }
      }
      .partitionMap(identity)

    val seenDidKeys = scala.collection.mutable.HashSet[(DidSuffix, String)]()
    val seenDidServices = scala.collection.mutable.HashSet[(DidSuffix, String)]()

    val filteredAllValid: List[(BlockProcessingInfo, SignedAtalaOperation, Operation)] =
      allValid.flatMap {
        case (blockInfo, protoOp, op @ CreateDIDOperation(_, keys, services, _, _, _)) =>
          val newKeys = keys.filter { key =>
            val tuple = (key.didSuffix, key.keyId)
            if (seenDidKeys.contains(tuple)) false
            else {
              seenDidKeys += tuple
              true
            }
          }
          val newServices = services.filter { service =>
            val tuple = (service.didSuffix, service.id)
            if (seenDidServices.contains(tuple)) false
            else {
              seenDidServices += tuple
              true
            }
          }
          if (newKeys.nonEmpty)
            Some((blockInfo, protoOp, op.copy(keys = newKeys, services = newServices)))
          else None

        case (blockInfo, protoOp, op @ UpdateDIDOperation(didSuffix, actions, _, _, _)) =>
          val newActions = actions.filter {
            case AddKeyAction(key) =>
              val tuple = (key.didSuffix, key.keyId)
              if (seenDidKeys.contains(tuple)) false
              else {
                seenDidKeys += tuple
                true
              }
            case AddServiceAction(service) =>
              val tuple = (service.didSuffix, service.id)
              if (seenDidServices.contains(tuple)) false
              else {
                seenDidServices += tuple
                true
              }
            case _ => true
          }
          if (newActions.nonEmpty)
            Some((blockInfo, protoOp, op.copy(actions = newActions)))
          else None

        case other => Some(other)
      }
    // // FOR DEBUGGING Only
    // val allDidKeyTuples: List[(DidSuffix, String, String)] =
    //   allValid.flatMap {
    //     case (_, _, CreateDIDOperation(_, keys, services, _, _, _)) =>
    //       keys.map(key => (key.didSuffix, key.keyId, "CreateDIDOperation")) ++
    //         services.map(service => (service.didSuffix, service.id, "CreateDIDOperation.AddServiceAction"))
    //     case (_, _, UpdateDIDOperation(_, actions, _, _, _)) =>
    //       actions.collect {
    //         case AddKeyAction(key) =>
    //           (key.didSuffix, key.keyId, "UpdateDIDOperation.AddKeyAction")
    //         case AddServiceAction(service) =>
    //           (service.didSuffix, service.id, "UpdateDIDOperation.AddServiceAction")
    //       }
    //     case (_, _, DeactivateDIDOperation(didSuffix, _, _, _)) =>
    //       List((didSuffix, "", "DeactivateDIDOperation"))
    //     case _ => Nil
    //   }
    // println("****************************************************************************************************")
    // println(s"allDidKeyTuples: ${allDidKeyTuples.mkString("\n")}")
    // println("****************************************************************************************************")

    for {
      // Create a single savepoint for all blocks
      savepoint <- connection.setSavepoint

      // Process all Invalid operations in a batch
      _ <- {
        if (allInvalid.isEmpty) connection.unit
        else {
          val errMsg =
            s"""
                   | Found invalid operations in txId: ${allInvalid
                .map(
                  _._1.transactionId
                )
                .mkString(",")}, ledger: ${allInvalid.map(_._1.ledger).mkString(",")}
                   | Operations:
                   |    ${allInvalid
                .map { case (_, op, err) => s"${err.render};\n\t${op.toProtoString.split("\n").mkString("\t\n")}" }
                .mkString(";\n\n\t")}
                   |""".stripMargin

          logger.warn(errMsg)

          AtalaOperationsDAO
            .updateAtalaOperationStatusBatch(
              allInvalid.map { case (_, op, _) => AtalaOperationId.of(op) },
              AtalaOperationStatus.REJECTED
            )
        }
      }

      // Process all valid operations in a batch
      opResults <- filteredAllValid.traverse { case (_, protoOperation, parsedOperation) =>
        val atalaOperationId = AtalaOperationId.of(protoOperation)
        for {
          atalaOperationInfo <- AtalaOperationsDAO.getAtalaOperationInfo(atalaOperationId)
          result <- processOperation(parsedOperation, protoOperation, atalaOperationId)
          _ <- result match {
            case Right(_) =>
              logRequestWithContext(
                methodName,
                s"Operation applied:\n${parsedOperation.digest}",
                atalaOperationId.toString,
                protoOperation
              )
              OperationsCounters.countOperationApplied(protoOperation)
              connection.unit
            case Left(err) =>
              logger.warn(
                s"Operation was not applied:\n${err.toString}\nOperation:\n${protoOperation.toProtoString}"
              )
              OperationsCounters.countOperationRejected(protoOperation, err)
              logRequestWithContext(
                methodName,
                s"Operation was not applied:\n${err.toString}",
                atalaOperationId.toString,
                protoOperation
              )
              if (atalaOperationInfo.exists(_.operationStatus != AtalaOperationStatus.APPLIED)) {
                AtalaOperationsDAO.updateAtalaOperationStatus(
                  atalaOperationId,
                  AtalaOperationStatus.REJECTED,
                  err.toString
                )
              } else connection.unit
          }
        } yield (atalaOperationId, result)
      }

      // If everything succeeded, release the savepoint
      _ <- connection.releaseSavepoint(savepoint)
    } yield opResults.exists(_._2.isRight)
  }
  // ConnectionIO[Boolean] is a temporary type used to be able to unit tests this
  // it eventually will be replaced with ConnectionIO[Unit]
  override def processBlock(
      block: node_models.AtalaBlock,
      transactionId: TransactionId,
      ledger: Ledger,
      blockTimestamp: Instant,
      blockIndex: Int
  ): ConnectionIO[Boolean] = {
    val methodName = "processBlock"
    val operations = block.operations.toList
    val operationsWithSeqNumbers = operations.zipWithIndex
    final case class ParsedOperations(
        valid: List[(SignedAtalaOperation, Operation)] = List.empty,
        invalid: List[(SignedAtalaOperation, ValidationError)] = List.empty
    )

    val parsedOperations = operationsWithSeqNumbers.foldRight(ParsedOperations()) { (v, acc) =>
      val (signedOperation, index) = v
      parseOperation(
        signedOperation,
        LedgerData(transactionId, ledger, TimestampInfo(blockTimestamp.toEpochMilli, blockIndex, index))
      ) match {
        case Left(err) => acc.copy(invalid = (signedOperation, err) :: acc.invalid)
        case Right(parsed) => acc.copy(valid = (signedOperation, parsed) :: acc.valid)
      }
    }

    val rejectInvalidOps = parsedOperations.invalid.pipe { invalidOps =>
      if (invalidOps.isEmpty) connection.unit
      else {
        val errMsg =
          s"""
             | Found invalid operations in txId: $transactionId, ledger: ${ledger.toString}
             | Operations:
             |    ${invalidOps
              .map { case (op, err) => s"${err.render};\n\t${op.toProtoString.split("\n").mkString("\t\n")}" }
              .mkString(";\n\n\t")}
             |""".stripMargin

        logger.warn(errMsg)

        AtalaOperationsDAO
          .updateAtalaOperationStatusBatch(
            invalidOps.map { case (op, _) => AtalaOperationId.of(op) },
            AtalaOperationStatus.REJECTED
          )
      }
    }

    val processValidOps = parsedOperations.valid.traverse { case (protoOperation, parsedOperation) =>
      val atalaOperationId = AtalaOperationId.of(protoOperation)
      val result: ConnectionIO[Unit] = for {
        // we want operations to be atomic - either it is applied correctly or the state is not modified
        // we are using SQL savepoints for that, which can be used to do subtransactions
        savepoint <- connection.setSavepoint
        atalaOperationInfo <- AtalaOperationsDAO.getAtalaOperationInfo(
          atalaOperationId
        )
        // verify signature, validate operation, and update the Node state by applied the operation
        // see BlockProcessingServiceImpl.processOperation for more details
        _ <- processOperation(
          parsedOperation,
          protoOperation,
          atalaOperationId
        )
          .flatMap {
            case Right(_) =>
              logRequestWithContext(
                methodName,
                s"Operation applied:\n${parsedOperation.digest}",
                atalaOperationId.toString,
                protoOperation
              )
              OperationsCounters.countOperationApplied(protoOperation)
              connection.releaseSavepoint(savepoint)
            case Left(err) =>
              logger.warn(
                s"Operation was not applied:\n${err.toString}\nOperation:\n${protoOperation.toProtoString}"
              )
              OperationsCounters.countOperationRejected(
                protoOperation,
                err
              )
              logRequestWithContext(
                methodName,
                s"Operation was not applied:\n${err.toString}",
                atalaOperationId.toString,
                protoOperation
              )
              // atomically rollback the operation
              connection
                .rollback(savepoint)
                .flatMap { _ =>
                  if ( // check that this operation wasn't applied before (so this is a duplicate)
                    atalaOperationInfo.exists(
                      _.operationStatus != AtalaOperationStatus.APPLIED
                    )
                  ) {
                    AtalaOperationsDAO
                      .updateAtalaOperationStatus(atalaOperationId, AtalaOperationStatus.REJECTED, err.toString)
                  } else {
                    connection.unit
                  }
                }
          }
      } yield ()
      result
    }

    for {
      _ <- rejectInvalidOps
      _ <- processValidOps
    } yield {
      // If some (at least one) operations have been processed, we consider the block processed
      // If all operations in the block ware invalid or the block was empty, then block was not processed
      if (operations.isEmpty || parsedOperations.valid.isEmpty) false
      else true
    }

  }

  // Apply operation and update the status of this operation
  def processOperation(
      operation: Operation,
      protoOperation: node_models.SignedAtalaOperation,
      atalaOperationId: AtalaOperationId
  ): ConnectionIO[Either[StateError, Unit]] = {
    val result = for {
      // Fetch key information and previous hash information
      correctnessData <- operation.getCorrectnessData(protoOperation.signedWith)
      key = correctnessData.key
      previousOperation = correctnessData.previousOperation
      // Verify that operation has the correct hash of the previous operation
      _ <- EitherT.cond[ConnectionIO](
        operation.linkedPreviousOperation == previousOperation,
        (),
        StateError.InvalidPreviousOperation(): StateError
      )
      // Verify signature of the operation
      _ <- EitherT.fromEither[ConnectionIO](
        verifySignature(key, protoOperation)
      )

      // Update Node's state
      _ <- operation.applyState(applyOperationConfig)
      // Set operation status to APPLIED
      _ <- EitherT.right[StateError](
        AtalaOperationsDAO.updateAtalaOperationStatus(
          atalaOperationId,
          AtalaOperationStatus.APPLIED
        )
      )
    } yield ()
    result.value
  }

  def verifySignature(
      key: SecpPublicKey,
      protoOperation: node_models.SignedAtalaOperation
  ): Either[StateError, Unit] = {
    try {
      Either.cond(
        SecpECDSA.checkECDSASignature(
          protoOperation.getOperation.toByteArray,
          protoOperation.signature.toByteArray,
          key
        ),
        (),
        StateError.InvalidSignature()
      )
    } catch {
      case ex: java.security.SignatureException =>
        logger.warn("Unable to parse signature", ex)
        Left(StateError.InvalidSignature())
      case NonFatal(ex) =>
        logNonFatalError(
          "verifySignature",
          "NonFatal Error",
          AtalaOperationId.of(protoOperation).toString,
          protoOperation,
          ex
        )
    }
  }

  private def logNonFatalError(
      methodName: String,
      message: String,
      operationId: String,
      request: node_models.SignedAtalaOperation,
      ex: Throwable
  ) = {
    logger.error(
      s"methodName:$methodName \n $message \n operationId = $operationId \n request = ${request.toProtoString}, \n Exception : $ex"
    )
    throw new RuntimeException(ex)
  }

  private def logRequestWithContext(
      methodName: String,
      message: String,
      operationId: String,
      request: node_models.SignedAtalaOperation
  ): Unit = {
    logger.info(
      s"methodName:$methodName, \n  $message \n operationId = $operationId \n request = \n ${request.toProtoString}"
    )
  }
}
