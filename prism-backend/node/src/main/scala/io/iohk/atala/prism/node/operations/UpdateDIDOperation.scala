package io.iohk.atala.prism.node.operations

import cats.data.{EitherT, OptionT}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.operations.StateError.EntityExists
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models

sealed trait UpdateDIDAction
case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction
case class RevokeKeyAction(keyId: String) extends UpdateDIDAction

case class UpdateDIDOperation(
    didSuffix: DIDSuffix,
    actions: List[UpdateDIDAction],
    previousOperation: SHA256Digest,
    digest: SHA256Digest,
    timestampInfo: TimestampInfo
) extends Operation {

  override def linkedPreviousOperation: Option[SHA256Digest] = Some(previousOperation)

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, SHA256Digest] {
        DIDDataDAO
          .getLastOperation(didSuffix)
          .map(_.toRight(StateError.EntityMissing("did suffix", didSuffix.value)))
      }
      key <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO.find(didSuffix, keyId).map(_.toRight(StateError.UnknownKey(didSuffix, keyId)))
      }.subflatMap { didKey =>
        Either.cond(didKey.keyUsage == KeyUsage.MasterKey, didKey.key, StateError.InvalidKeyUsed("master key"))
      }
      _ <- EitherT.fromEither[ConnectionIO] {
        val revokedKeyIds = actions.collect { case RevokeKeyAction(id) => id }
        Either.cond(!(revokedKeyIds contains keyId), (), StateError.InvalidRevocation(): StateError)
      }
    } yield CorrectnessData(key, Some(lastOperation))
  }

  protected def applyAction(action: UpdateDIDAction): EitherT[ConnectionIO, StateError, Unit] = {
    action match {
      case AddKeyAction(key) =>
        EitherT {
          PublicKeysDAO.insert(key, timestampInfo).attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              EntityExists("DID suffix", didSuffix.value): StateError
          }
        }
      case RevokeKeyAction(keyId) =>
        EitherT.right[StateError](PublicKeysDAO.revoke(keyId, timestampInfo)).subflatMap { wasRemoved =>
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

    for {
      _ <-
        OptionT(DIDDataDAO.findByDidSuffix(didSuffix)).toRight(StateError.EntityMissing("DID Suffix", didSuffix.value))
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
      } yield RevokeKeyAction(keyId)
    } else {
      Left(action.child(_.action, "action").missing())
    }
  }

  /** Parses the protobuf representation of operation
    *
    * @param signedOperation signed operation, needs to be of the type compatible with the called companion object
    * @param timestampInfo timestamp information provided by the caller, needed to instantiate the operation objects
    * @return parsed operation or ValidationError signifying the operation is invalid
    */
  override def parse(
      signedOperation: node_models.SignedAtalaOperation,
      timestampInfo: TimestampInfo
  ): Either[ValidationError, UpdateDIDOperation] = {
    val operation = signedOperation.getOperation
    val signingKeyId = signedOperation.signedWith

    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val updateOperation = ValueAtPath(operation, Path.root).child(_.getUpdateDid, "updateDid")

    for {
      didSuffix <- updateOperation.child(_.id, "id").parse { didSuffix =>
        Either.fromOption(
          DIDSuffix.fromString(didSuffix),
          s"must be a valid DID suffix: $didSuffix"
        )
      }
      previousOperation <- ParsingUtils.parseHash(
        updateOperation.child(_.previousOperationHash, "previousOperationHash")
      )
      reversedActions <-
        updateOperation
          .children(_.actions, "actions")
          .foldLeft[Either[ValidationError, List[UpdateDIDAction]]](Right(Nil)) {
            case (eitherAcc, action) =>
              for {
                acc <- eitherAcc
                parsedAction <- parseAction(action, didSuffix, signingKeyId)
              } yield parsedAction :: acc
          }
    } yield UpdateDIDOperation(didSuffix, reversedActions.reverse, previousOperation, operationDigest, timestampInfo)
  }
}
