package io.iohk.atala.prism.node.operations

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, DIDServiceEndpoint, KeyUsage, nodeState}
import io.iohk.atala.prism.node.operations.StateError.EntityExists
import io.iohk.atala.prism.node.operations.ValidationError.{MissingAtLeastOneValue, MissingValue}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, ServicesDAO}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.UpdateDIDAction.Action

sealed trait UpdateDIDAction
case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction
case class RevokeKeyAction(keyId: String) extends UpdateDIDAction
case class AddServiceAction(service: DIDService) extends UpdateDIDAction

// id, not to be confused with internal service_id in db, this is service.id
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

  private def revokeService(didSuffix: DidSuffix, id: String, ledgerData: LedgerData) = {
    EitherT
      .right[StateError](
        ServicesDAO.revokeService(didSuffix, id, ledgerData)
      )
      .subflatMap { wasRemoved =>
        Either.cond(
          wasRemoved,
          (),
          StateError.EntityMissing("service", s"${didSuffix.getValue} - $id"): StateError
        )
      }
  }

  private def createService(service: DIDService, ledgerData: LedgerData) = {
    EitherT.apply {
      ServicesDAO.insert(service, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        EntityExists("service", s"${service.didSuffix.getValue} - ${service.id}"): StateError
      }
    }
  }

  private def getService(didSuffix: DidSuffix, id: String): ConnectionIO[Option[DIDService]] = {

    for {
      maybeDidServiceState <- ServicesDAO.get(didSuffix, id)
      didService = maybeDidServiceState.map(x =>
        DIDService(
          id = x.id,
          didSuffix = x.didSuffix,
          `type` = x.`type`,
          serviceEndpoints = x.serviceEndpoints.map(s => DIDServiceEndpoint(s.urlIndex, s.url))
        )
      )
    } yield didService
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
          _ <- EitherT
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
        } yield ()
      case AddServiceAction(service) => createService(service, ledgerData)

      case RemoveServiceAction(id) => revokeService(didSuffix, id, ledgerData)

      case UpdateServiceAction(id, serviceType, serviceEndpoints) =>
        /*
         * revoke the current service and create a new one with
         * provided service endpoints, this approach of updating service is
         * chosen to preserve the history of the service updates and which service
         * endpoints used to be associated with which instance.
         */

        // get service
        // if found
        //  revoke current service
        //  if type is provided, replace type, if not use old type
        //  if serviceEndpoints are provided, use them, otherwise use old ones
        //  create new DIDService
        //  Note: either type or serviceEndpoints must be provided

        for {
          service <- EitherT.right[StateError](getService(didSuffix, id)).subflatMap {
            case Some(value) => Right(value)
            case None => Left(StateError.EntityMissing("service", s"${didSuffix.getValue} - $id"): StateError)
          }
          _ <- revokeService(didSuffix, id, ledgerData)
          newServiceType =
            if (serviceType.nonEmpty) serviceType else service.`type` // use old type if new is not provided (no update)
          newServiceEndpoints =
            if (serviceEndpoints.nonEmpty) serviceEndpoints
            else service.serviceEndpoints // use old service endpoints if new ones are not provided
          newService = DIDService(id, didSuffix, newServiceType, newServiceEndpoints)
          _ <- createService(newService, ledgerData)
        } yield ()
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
            .parseServiceId(
              ValueAtPath(value.serviceId, path)
            )
            .map(RemoveServiceAction)

          removeServiceAction

        case Action.UpdateService(value) =>
          val path = action.path / "updateService"

          for {
            id <- ParsingUtils.parseServiceId(
              ValueAtPath(value.serviceId, path / "serviceId")
            )
            serviceType = value.`type` // if empty then do not update
            serviceEndpoints <- ParsingUtils.parseServiceEndpoints(
              ValueAtPath(value.serviceEndpoints.toList, path / "serviceEndpoints"),
              id,
              validateEmpty = false
            )
            _ <-
              if (serviceType.isEmpty && serviceEndpoints.isEmpty) {
                val typePath = path / "type"
                val serviceEndpointsPath = path / "serviceEndpoints"
                Left(
                  MissingAtLeastOneValue(NonEmptyList(typePath, List(serviceEndpointsPath)))
                )
              } else Right(())
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
