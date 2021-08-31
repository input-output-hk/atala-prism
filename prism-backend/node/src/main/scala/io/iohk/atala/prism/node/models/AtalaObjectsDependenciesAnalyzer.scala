package io.iohk.atala.prism.node.models

import cats.implicits._

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.AtalaObjectsDependenciesAnalyzer._
import io.iohk.atala.prism.node.operations.{
  AddKeyAction,
  CreateDIDOperation,
  IssueCredentialBatchOperation,
  Operation,
  RevokeCredentialsOperation,
  RevokeKeyAction,
  UpdateDIDOperation
}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

class AtalaObjectsDependenciesAnalyzer(atalaObjects: List[AtalaObjectInfo]) {
  private def getOperationByIndex(pos: OperationPosition): Operation =
    atalaObjects(pos.objectIndex).operations(pos.operationIndex)

  private val digestToIndex: Map[SHA256Digest, OperationPosition] = Map.from(
    for {
      (atalaObject, objectIndex) <- atalaObjects.zipWithIndex
      (op, opIndex) <- atalaObject.operations.zipWithIndex
    } yield op.digest -> OperationPosition(objectIndex, opIndex)
  )

  private val previousOpsDependencies: Map[OperationPosition, OperationPosition] = Map.from {
    val operationDependenciesList = for {
      (atalaObjectInfo, objectIndex) <- atalaObjects.zipWithIndex
      (op, opIndex) <- atalaObjectInfo.operations.zipWithIndex
      curOperationPosition = OperationPosition(objectIndex, opIndex)
      previousOperationDigest <- op.linkedPreviousOperation
      previousOperationPosition <- digestToIndex.get(previousOperationDigest)
    } yield curOperationPosition -> previousOperationPosition
    operationDependenciesList
  }

  // TODO: this requires invariant that all operations couldn't go before its predecessor -- we can easily verify this in sendAtalaOperations method
  private val rootOperations: collection.mutable.Map[OperationPosition, OperationPosition] =
    collection.mutable.Map.empty
  private def calcRoot(op: OperationPosition): Unit = {
    val root = previousOpsDependencies.get(op).fold(op)(rootOperations)
    rootOperations.update(op, root)
  }
  for {
    (atalaObject, objectIndex) <- atalaObjects.zipWithIndex
    opIndex <- atalaObject.operations.indices
  } yield calcRoot(OperationPosition(objectIndex, opIndex))

  private val keyOperationsList: List[KeyOperation] = for {
    (atalaObjectInfo, objectIndex) <- atalaObjects.zipWithIndex
    (operation, opIndex) <- atalaObjectInfo.operations.zipWithIndex
    curOperation = OperationPosition(objectIndex, opIndex)
    (isAddition, didId, keyId) <- operation match {
      case CreateDIDOperation(didId, keys, _, _) =>
        keys.map { didPublicKey =>
          (true, didId.getValue, didPublicKey.keyId)
        }

      case UpdateDIDOperation(didId, actions, _, _, _) =>
        actions.map {
          case AddKeyAction(didPublicKey) =>
            (true, didId.getValue, didPublicKey.keyId)
          case RevokeKeyAction(keyId) =>
            (false, didId.getValue, keyId)
        }
      case _ =>
        List.empty
    }
  } yield KeyOperation(isAddition, (didId, keyId), curOperation)

  private val (
    keyAdditionToOperation,
    keyDeletionToOperation
  ): (Map[KeyIndex, OperationPosition], Map[KeyIndex, OperationPosition]) =
    keyOperationsList
      .partition { _.isAddition }
      .bimap(
        additions => Map.from(additions map { keyOp => keyOp.key -> keyOp.operationPosition }),
        deletions => Map.from(deletions map { keyOp => keyOp.key -> keyOp.operationPosition })
      )

  private def getDidFromOperation(operationIndex: OperationPosition): Option[DidId] = {
    val rootOperationIndex = rootOperations(operationIndex)
    getOperationByIndex(rootOperationIndex) match {
      case CreateDIDOperation(id, _, _, _) =>
        Some(id.getValue)
      case UpdateDIDOperation(id, _, _, _, _) =>
        Some(id.getValue)
      case IssueCredentialBatchOperation(_, id, _, _, _) =>
        Some(id.getValue)
      case RevokeCredentialsOperation(_, _, _, _, _) =>
        None
    }
  }

  private val keyOperationDependencies: Map[OperationPosition, OperationPosition] = Map.from {
    val dependenciesList = for {
      (atalaObjectInfo, objectIndex) <- atalaObjects.zipWithIndex

      (signedAtalaOperation, opIndex) <- atalaObjectInfo.getAtalaBlock.fold(
        List.empty[(SignedAtalaOperation, Int)]
      ) { atalaBlock =>
        atalaBlock.operations.toList.zipWithIndex
      }
      curOperation = OperationPosition(objectIndex, opIndex)

      didId <- getDidFromOperation(curOperation)
      keyIndex = (didId, signedAtalaOperation.signedWith: KeyId)
      keyAdditionOperation = keyAdditionToOperation.get(keyIndex)
      keyDeletionOperation = keyDeletionToOperation.get(keyIndex)
    } yield List(
      keyAdditionOperation.map(curOperation -> _), // key addition should go before key usage
      keyDeletionOperation.map(_ -> curOperation) // key revocation should go after key usage
    ).flatten
    dependenciesList.flatten
  }

  private lazy val previousOpsDependenciesInverses = previousOpsDependencies.filter {
    case (curOpPosition, prevOpPosition) =>
      curOpPosition < prevOpPosition
  }
  private lazy val keyOperationDependenciesInverses = keyOperationDependencies.filter {
    case (curOpPosition, prevOpPosition) =>
      curOpPosition < prevOpPosition
  }

  lazy val dependencyInverses: List[(SHA256Digest, SHA256Digest)] =
    (previousOpsDependenciesInverses ++ keyOperationDependenciesInverses).toList
      .map {
        case (curOperationIndex, prevOperationIndex) =>
          (getOperationByIndex(curOperationIndex).digest, getOperationByIndex(prevOperationIndex).digest)
      }
}

object AtalaObjectsDependenciesAnalyzer {
  type DidId = String
  type KeyId = String
  type KeyIndex = (DidId, KeyId)

  case class OperationPosition(objectIndex: Int, operationIndex: Int) {
    def <(that: OperationPosition): Boolean =
      (objectIndex < that.objectIndex) ||
        (objectIndex == that.objectIndex && operationIndex < that.operationIndex)
  }

  case class KeyOperation(isAddition: Boolean, key: KeyIndex, operationPosition: OperationPosition)
}
