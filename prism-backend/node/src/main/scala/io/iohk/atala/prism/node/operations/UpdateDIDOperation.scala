package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage, nodeState}
import io.iohk.atala.prism.node.operations.StateError.EntityExists
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

sealed trait UpdateDIDAction
case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction
case class RevokeKeyAction(keyId: String) extends UpdateDIDAction

case class UpdateDIDOperation(
    didSuffix: DidSuffix,
    actions: List[UpdateDIDAction],
    previousOperation: Sha256Digest,
    digest: Sha256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {

  override def linkedPreviousOperation: Option[Sha256Digest] = Some(previousOperation)

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, Sha256Digest] {
        DIDDataDAO
          .getLastOperation(didSuffix)
          .map(_.toRight(StateError.EntityMissing("did suffix", didSuffix.getValue)))
      }
      key <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO.find(didSuffix, keyId).map(_.toRight(StateError.UnknownKey(didSuffix, keyId)))
      }.subflatMap { didKey =>
        Either.cond(didKey.keyUsage == KeyUsage.MasterKey, didKey.key, StateError.InvalidKeyUsed("master key"))
      }
    } yield CorrectnessData(key, Some(lastOperation))
  }

  protected def applyAction(action: UpdateDIDAction): EitherT[ConnectionIO, StateError, Unit] = {
    action match {
      case AddKeyAction(key) =>
        EitherT {
          PublicKeysDAO.insert(key, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("DID suffix", didSuffix.getValue): StateError
          }
        }
      case RevokeKeyAction(keyId) =>
        for {
          _ <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
            PublicKeysDAO.find(didSuffix, keyId).map(_.toRight(StateError.EntityMissing("key", keyId)))
          }.subflatMap { didKey =>
            Either.cond(didKey.revokedOn.isEmpty, didKey.key, StateError.KeyAlreadyRevoked())
          }
          result <- EitherT.right[StateError](PublicKeysDAO.revoke(didSuffix, keyId, ledgerData)).subflatMap {
            wasRemoved =>
              Either.cond(wasRemoved, (), StateError.EntityMissing("key", keyId))
          }
        } yield result
    }
  }

  /** Applies operation to the state
    *
    * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
    */
  override def applyStateImpl(): EitherT[ConnectionIO, StateError, Unit] = {
    // type lambda T => EitherT[ConnectionIO, StateError, T]
    // in .traverse we need to express what Monad is to be used
    // as EitherT has 3 type parameters, it cannot be deduced from the context
    // we need to create a way to construct the Monad from the underlying type T
    type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

    for {
      countUpdated <- EitherT.right(DIDDataDAO.updateLastOperation(didSuffix, digest))
      _ <- EitherT.cond[ConnectionIO](
        countUpdated == 1,
        unit,
        StateError.EntityMissing("DID Suffix", didSuffix.getValue)
      )
      _ <- actions.traverse[ConnectionIOEitherTError, Unit](applyAction)
    } yield ()
  }
}

object UpdateDIDOperation extends OperationCompanion[UpdateDIDOperation] {

  protected def parseAction(
      action: ValueAtPath[node_models.UpdateDIDAction],
      didSuffix: DidSuffix
  ): Either[ValidationError, UpdateDIDAction] = {
    if (action(_.action.isAddKey)) {
      val addKeyAction = action.child(_.getAddKey, "addKey")
      for {
        key <- addKeyAction.childGet(_.key, "key").flatMap(ParsingUtils.parseKey(_, didSuffix))
      } yield AddKeyAction(key)
    } else if (action(_.action.isRemoveKey)) {
      val removeKeyAction = action.child(_.getRemoveKey, "removeKey")
      val keyIdVal = removeKeyAction.child(_.keyId, "keyId")
      for {
        keyId <- ParsingUtils.parseKeyId(keyIdVal)
      } yield RevokeKeyAction(keyId)
    } else {
      Left(action.child(_.action, "action").missing())
    }
  }

  /** Parses the protobuf representation of operation
    *
    * @param operation
    *   operation, needs to be of the type compatible with the called companion object
    * @param ledgerData
    *   ledger information provided by the caller, needed to instantiate the operation objects
    * @return
    *   parsed operation or ValidationError signifying the operation is invalid
    */
  override def parse(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, UpdateDIDOperation] = {
    val operationDigest = Sha256.compute(operation.toByteArray)
    val updateOperation = ValueAtPath(operation, Path.root).child(_.getUpdateDid, "updateDid")

    for {
      didSuffix <- updateOperation.child(_.id, "id").parse { didSuffix =>
        DidSuffix.fromString(didSuffix).toEither.left.map(_.getMessage)
      }
      previousOperation <- ParsingUtils.parseHash(
        updateOperation.child(_.previousOperationHash, "previousOperationHash")
      )
      reversedActions <-
        updateOperation
          .children(_.actions, "actions")
          .foldLeft[Either[ValidationError, List[UpdateDIDAction]]](Right(Nil)) { case (eitherAcc, action) =>
            for {
              acc <- eitherAcc
              parsedAction <- parseAction(action, didSuffix)
            } yield parsedAction :: acc
          }
    } yield UpdateDIDOperation(didSuffix, reversedActions.reverse, previousOperation, operationDigest, ledgerData)
  }
}
