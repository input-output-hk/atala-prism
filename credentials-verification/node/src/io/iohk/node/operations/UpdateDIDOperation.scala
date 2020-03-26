package io.iohk.node.operations

import cats.data.{EitherT, OptionT}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.models.{DIDPublicKey, DIDSuffix, KeyUsage}
import io.iohk.node.operations.StateError.EntityExists
import io.iohk.node.operations.path._
import io.iohk.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.prism.protos.node_models

sealed trait UpdateDIDAction
case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction
case class RemoveKeyAction(keyId: String) extends UpdateDIDAction

case class UpdateDIDOperation(
    id: DIDSuffix,
    actions: List[UpdateDIDAction],
    previousOperation: SHA256Digest,
    digest: SHA256Digest
) extends Operation {

  override def linkedPreviousOperation: Option[SHA256Digest] = Some(previousOperation)

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, SHA256Digest] {
        DIDDataDAO
          .getLastOperation(id)
          .map(_.toRight(StateError.EntityMissing("did", id.toString)))
      }
      key <- EitherT[ConnectionIO, StateError, DIDPublicKey] {
        PublicKeysDAO.find(id, keyId).map(_.toRight(StateError.UnknownKey(id, keyId)))
      }.subflatMap { didKey =>
        Either.cond(didKey.keyUsage == KeyUsage.MasterKey, didKey.key, StateError.InvalidKeyUsed("master key"))
      }
    } yield CorrectnessData(key, Some(lastOperation))
  }

  protected def applyAction(action: UpdateDIDAction): EitherT[ConnectionIO, StateError, Unit] = {
    action match {
      case AddKeyAction(key) =>
        EitherT {
          PublicKeysDAO.insert(key).attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              EntityExists("DID", id.suffix): StateError
          }
        }
      case RemoveKeyAction(keyId) =>
        EitherT.right[StateError](PublicKeysDAO.remove(keyId)).subflatMap { wasRemoved =>
          Either.cond(wasRemoved, (), StateError.EntityMissing("key", keyId))
        }
    }
  }

  /** Applies operation to the state
    *
    * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
    */
  override def applyState(): EitherT[ConnectionIO, StateError, Unit] = {
    // type lambda T => EitherT[ConnectionIO, StateError, T]
    // in .traverse we need to express what Monad is to be used
    // as EitherT has 3 type parameters, it cannot be deduced from the context
    // we need to create a way to construct the Monad from the underlying type T
    type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

    // there are two implicit implementations for cats.Monad[doobie.free.connection.ConnectionIO],
    // one from doobie, the other for cats, making it ambiguous
    // we need to choose one
    implicit def _connectionIOMonad: cats.Monad[doobie.free.connection.ConnectionIO] =
      doobie.free.connection.AsyncConnectionIO

    for {
      _ <- OptionT(DIDDataDAO.findByDidSuffix(id)).toRight(StateError.EntityMissing("DID", id.suffix))
      _ <- actions.traverse[ConnectionIOEitherTError, Unit](applyAction)
    } yield ()
  }
}

object UpdateDIDOperation extends OperationCompanion[UpdateDIDOperation] {

  protected def parseAction(
      action: ValueAtPath[node_models.UpdateDIDAction],
      didSuffix: DIDSuffix,
      signingKeyId: String
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
        _ <- Either.cond(keyId != signingKeyId, (), keyIdVal.invalid("Cannot remove key used to sign operation"))
      } yield RemoveKeyAction(keyId)
    } else {
      Left(action.child(_.action, "action").missing())
    }
  }

  /** Parses the protobuf representation of operation
    *
    * @param signedOperation signed operation, needs to be of the type compatible with the called companion object
    * @return parsed operation or ValidationError signifying the operation is invalid
    */
  override def parse(signedOperation: node_models.SignedAtalaOperation): Either[ValidationError, UpdateDIDOperation] = {
    val operation = signedOperation.getOperation
    val signingKeyId = signedOperation.signedWith

    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val updateOperation = ValueAtPath(operation, Path.root).child(_.getUpdateDid, "updateDid")

    for {
      didSuffix <- updateOperation.child(_.id, "id").parse { didSuffix =>
        Either.cond(
          DIDSuffix.DID_SUFFIX_RE.pattern.matcher(didSuffix).matches(),
          DIDSuffix(didSuffix),
          "must be a valid DID suffix"
        )
      }
      previousOperation <- ParsingUtils.parseHash(
        updateOperation.child(_.previousOperationHash, "previousOperationHash")
      )
      reversedActions <- updateOperation
        .children(_.actions, "actions")
        .foldLeft[Either[ValidationError, List[UpdateDIDAction]]](Right(Nil)) {
          case (eitherAcc, action) =>
            for {
              acc <- eitherAcc
              parsedAction <- parseAction(action, didSuffix, signingKeyId)
            } yield parsedAction :: acc
        }
    } yield UpdateDIDOperation(didSuffix, reversedActions.reverse, previousOperation, operationDigest)
  }
}
