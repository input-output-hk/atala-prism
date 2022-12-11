package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.{DidSuffix, IdType}
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, DIDServiceEndpoint, KeyUsage, nodeState}
import io.iohk.atala.prism.node.operations.StateError.EntityExists
import io.iohk.atala.prism.node.operations.ValidationError.MissingValue
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.UpdateDIDAction.Action

sealed trait UpdateDIDAction
case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction
case class RevokeKeyAction(keyId: String) extends UpdateDIDAction
case class AddServiceAction(service: DIDService) extends UpdateDIDAction

// id, not to be confused with internal service_id in db, this is services.id
case class RemoveServiceAction(id: String) extends UpdateDIDAction
case class UpdateServiceAction(id: String, `type`: String, serviceEndpoints: List[DIDServiceEndpoint])
    extends UpdateDIDAction

case class UpdateDIDOperation(
    didSuffix: DidSuffix,
    actions: List[UpdateDIDAction],
    previousOperation: Sha256Digest,
    digest: Sha256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {
  override val metricCounterName: String = UpdateDIDOperation.metricCounterName

  override def linkedPreviousOperation: Option[Sha256Digest] = Some(
    previousOperation
  )

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, Sha256Digest] {
        DIDDataDAO
          .getLastOperation(didSuffix)
          .map(
            _.toRight(
              StateError.EntityMissing("did suffix", didSuffix.getValue)
            )
          )
      }
      key <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(didSuffix, keyId)
          .map(_.toRight(StateError.UnknownKey(didSuffix, keyId)))
      }.subflatMap { didKey =>
        Either.cond(
          didKey.keyUsage == KeyUsage.MasterKey,
          didKey,
          StateError.InvalidKeyUsed("master key")
        )
      }.subflatMap { didKey =>
        Either.cond(
          didKey.revokedOn.isEmpty,
          didKey,
          StateError.KeyAlreadyRevoked()
        )
      }.map(_.key)
    } yield CorrectnessData(key, Some(lastOperation))
  }

  protected def applyAction(
      action: UpdateDIDAction
  ): EitherT[ConnectionIO, StateError, Unit] = {
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
            PublicKeysDAO
              .find(didSuffix, keyId)
              .map(_.toRight(StateError.EntityMissing("key", keyId)))
          }.subflatMap { didKey =>
            Either.cond(
              didKey.revokedOn.isEmpty,
              didKey.key,
              StateError.KeyAlreadyRevoked()
            )
          }
          result <- EitherT
            .right[StateError](
              PublicKeysDAO.revoke(didSuffix, keyId, ledgerData)
            )
            .subflatMap { wasRemoved =>
              Either.cond(
                wasRemoved,
                (),
                StateError.EntityMissing("key", keyId)
              )
            }
        } yield result
    }
  }

  /** Applies operation to the state
    *
    * It's the responsibility of the caller to manage transaction, in order to ensure atomicity of the operation.
    */
  override def applyStateImpl(_config: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] = {
    // type lambda T => EitherT[ConnectionIO, StateError, T]
    // in .traverse we need to express what Monad is to be used
    // as EitherT has 3 type parameters, it cannot be deduced from the context
    // we need to create a way to construct the Monad from the underlying type T
    type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

    for {
      countUpdated <- EitherT.right(
        DIDDataDAO.updateLastOperation(didSuffix, digest)
      )
      _ <- EitherT.cond[ConnectionIO](
        countUpdated == 1,
        unit,
        StateError.EntityMissing("DID Suffix", didSuffix.getValue)
      )
      _ <- actions.traverse[ConnectionIOEitherTError, Unit](applyAction)
      _ <- EitherT[ConnectionIO, StateError, Unit](
        PublicKeysDAO
          .findAll(didSuffix)
          .map { keyList =>
            Either.cond(
              keyList.exists(key => key.keyUsage == KeyUsage.MasterKey && key.revokedOn.isEmpty),
              (),
              StateError.InvalidMasterKeyRevocation()
            )
          }
      )
    } yield ()
  }
}

object UpdateDIDOperation extends OperationCompanion[UpdateDIDOperation] {
  val metricCounterName: String = "number_of_did_updates"

  protected def parseAction(
      action: ValueAtPath[node_models.UpdateDIDAction],
      didSuffix: DidSuffix
  ): Either[ValidationError, UpdateDIDAction] = {

    action { uda =>
      uda.action match {
        case Action.AddKey(value) =>
          val path = action.path / "addKey" / "key"
          val addKeyAction = value.key
            .toRight(MissingValue(path))
            .map(ValueAtPath(_, path))
            .flatMap(ParsingUtils.parseKey(_, didSuffix))
            .map(AddKeyAction)

          addKeyAction

        case Action.RemoveKey(value) =>
          val path = action.path / "removeKey" / "keyId"
          val removeKeyAction = ParsingUtils
            .parseKeyId(
              ValueAtPath(value.keyId, path)
            )
            .map(RevokeKeyAction)

          removeKeyAction

        case Action.AddService(value) =>
          val path = action.path / "addService" / "service"
          val addServiceAction = value.service
            .toRight(MissingValue(path))
            .map(ValueAtPath(_, path))
            .flatMap(ParsingUtils.parseService(_, didSuffix))
            .map(AddServiceAction)

          addServiceAction

        case Action.RemoveService(value) =>
          val path = action.path / "removeService" / "serviceId"
          val removeServiceAction = ParsingUtils
            .parseKeyId(
              ValueAtPath(value.serviceId, path)
            )
            .map(RemoveServiceAction)

          removeServiceAction

        case Action.UpdateService(value) =>
          val path = action.path / "updateService"

          for {
            id <- ParsingUtils.parseKeyId(
              ValueAtPath(value.serviceId, path / "serviceId")
            )
            serviceType <- ParsingUtils.parseServiceType(ValueAtPath(value.`type`, path / "type"))
            serviceEndpoints <- ParsingUtils.parseServiceEndpoints(
              ValueAtPath(value.serviceEndpoints, path / "serviceEndpoints"),
              id
            )
          } yield UpdateServiceAction(id, serviceType, serviceEndpoints)

        case Action.Empty => Left(action.child(_.action, "action").missing())

      }
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
    val updateOperation =
      ValueAtPath(operation, Path.root).child(_.getUpdateDid, "updateDid")

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
          .foldLeft[Either[ValidationError, List[UpdateDIDAction]]](
            Right(Nil)
          ) { case (eitherAcc, action) =>
            for {
              acc <- eitherAcc
              parsedAction <- parseAction(action, didSuffix)
            } yield parsedAction :: acc
          }
    } yield UpdateDIDOperation(
      didSuffix,
      reversedActions.reverse,
      previousOperation,
      operationDigest,
      ledgerData
    )
  }
}
