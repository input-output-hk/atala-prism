package io.iohk.atala.prism.node.operations

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.StateError.IllegalSecp256k1Key
import io.iohk.atala.prism.node.operations.ValidationError.{MissingAtLeastOneValue, MissingValue}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{ContextDAO, DIDDataDAO, PublicKeysDAO, ServicesDAO}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.UpdateDIDAction.Action

import scala.util.Try

sealed trait UpdateDIDAction

case class AddKeyAction(key: DIDPublicKey) extends UpdateDIDAction

case class RevokeKeyAction(keyId: String) extends UpdateDIDAction

case class AddServiceAction(service: DIDService) extends UpdateDIDAction

// id, not to be confused with internal service_id in db, this is service.id
case class RemoveServiceAction(id: String) extends UpdateDIDAction

case class UpdateServiceAction(id: String, `type`: Option[String], serviceEndpoints: Option[String])
    extends UpdateDIDAction

case class PatchContextAction(context: List[String]) extends UpdateDIDAction

case class UpdateDIDOperation(
    didSuffix: DidSuffix,
    actions: List[UpdateDIDAction],
    previousOperation: Sha256Hash,
    digest: Sha256Hash,
    ledgerData: nodeState.LedgerData
) extends Operation {
  override val metricCounterName: String = UpdateDIDOperation.metricCounterName

  override def linkedPreviousOperation: Option[Sha256Hash] = Some(
    previousOperation
  )

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, Sha256Hash] {
        DIDDataDAO
          .getLastOperation(didSuffix)
          .map(
            _.toRight(
              StateError.EntityMissing("did suffix", didSuffix.getValue)
            )
          )
      }
      keyData <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
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
      secpKey <- EitherT.fromEither[ConnectionIO] {
        val tryKey = Try {
          SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(keyData.compressedKey)
        }
        tryKey.toOption
          .toRight(IllegalSecp256k1Key(keyId): StateError)
      }
    } yield CorrectnessData(secpKey, Some(lastOperation))
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
    EitherT {
      ServicesDAO.insert(service, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        StateError.EntityExists("service", s"${service.didSuffix.getValue} - ${service.id}"): StateError
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
          serviceEndpoints = x.serviceEndpoints
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
            StateError.EntityExists("DID suffix", didSuffix.getValue): StateError
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
          newServiceType = serviceType.getOrElse(service.`type`) // use old type if new is not provided (no update)
          newServiceEndpoints = serviceEndpoints.getOrElse(
            service.serviceEndpoints
          ) // use old service endpoints if new ones are not provided
          newService = DIDService(id, didSuffix, newServiceType, newServiceEndpoints)
          _ <- createService(newService, ledgerData)
        } yield ()

      case PatchContextAction(context) =>
        // Processing PatchContextAction If the DID to update has an empty context associated to it in the map:
        // * the field context MUST NOT be empty and MUST NOT contain repeated values Update of the internal map
        //
        // If context is empty, the DID removes the previous context list associated to it.
        // If context is not empty, the DID replaces the old list for the new one on its map.

        type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

        for {
          contextFromDb <- EitherT.right[StateError](ContextDAO.getAllActiveByDidSuffix(didSuffix))
          // if the context provided is empty AND context in db is also empty, return an error
          _ <- EitherT.fromEither[ConnectionIO] {
            if (contextFromDb.isEmpty && context.isEmpty)
              Left(StateError.EntityMissing("context", s"${didSuffix.getValue}"))
            else Right(())
          }
          _ <- EitherT.right[StateError](ContextDAO.revokeAllContextStrings(didSuffix, ledgerData))

          // If context is empty, won't insert anything, which is the goal
          _ <- context.traverse[ConnectionIOEitherTError, Unit] { contextStr: String =>
            EitherT {
              ContextDAO.insert(contextStr, didSuffix, ledgerData).attemptSomeSqlState {
                case sqlstate.class23.UNIQUE_VIOLATION =>
                  StateError.EntityExists("context string", s"${didSuffix.getValue} - $contextStr"): StateError
              }
            }
          }
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

      publicKeysLimit = ProtocolConstants.publicKeysLimit
      servicesLimit = ProtocolConstants.servicesLimit

      keys <- EitherT.right[StateError](PublicKeysDAO.listAllNonRevoked(didSuffix))
      _ <- EitherT.fromEither[ConnectionIO] {
        if (keys.length > publicKeysLimit)
          Left(
            StateError.ExceededPublicKeyLimit(
              s"Trying to add a key to a DID that already has maximum amount of keys allowed - $publicKeysLimit"
            )
          )
        else Right(())
      }

      services <- EitherT.right[StateError](ServicesDAO.getAllActiveByDidSuffix(didSuffix))
      _ <- EitherT.fromEither[ConnectionIO] {
        if (services.length > servicesLimit)
          Left(
            StateError.ExceededServicesLimit(
              s"Trying to add a service to a DID that already has maximum amount of services allowed - $servicesLimit"
            )
          )
        else Right(())
      }

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

    val serviceEndpointCharLenLimit = ProtocolConstants.serviceEndpointCharLenLimit
    val serviceTypeCharLimit = ProtocolConstants.serviceTypeCharLimit
    val contextStringCharLimit = ProtocolConstants.contextStringCharLimit
    val idCharLenLimit = ProtocolConstants.idCharLenLimit

    action { uda =>
      uda.action match {
        case Action.AddKey(value) =>
          val path = action.path / "addKey" / "key"
          value.key
            .toRight(MissingValue(path))
            .map(ValueAtPath(_, path))
            .flatMap(ParsingUtils.parseKey(_, didSuffix, idCharLenLimit))
            .map(AddKeyAction)

        case Action.RemoveKey(value) =>
          val path = action.path / "removeKey" / "keyId"
          ParsingUtils
            .parseKeyId(
              ValueAtPath(value.keyId, path),
              idCharLenLimit
            )
            .map(RevokeKeyAction)

        case Action.AddService(value) =>
          val path = action.path / "addService" / "service"
          value.service
            .toRight(MissingValue(path))
            .map(ValueAtPath(_, path))
            .flatMap(
              ParsingUtils.parseService(_, didSuffix, serviceEndpointCharLenLimit, serviceTypeCharLimit, idCharLenLimit)
            )
            .map(AddServiceAction)

        case Action.RemoveService(value) =>
          val path = action.path / "removeService" / "serviceId"
          ParsingUtils
            .parseServiceId(
              ValueAtPath(value.serviceId, path),
              idCharLenLimit
            )
            .map(RemoveServiceAction)

        case Action.UpdateService(value) =>
          val path = action.path / "updateService"

          for {
            id <- ParsingUtils.parseServiceId(
              ValueAtPath(value.serviceId, path / "serviceId"),
              idCharLenLimit
            )

            serviceType <- ParsingUtils.parseServiceType(
              serviceType = ValueAtPath(value.`type`, path / "type"),
              canBeEmpty = true,
              serviceTypeCharLimit = serviceTypeCharLimit
            )
            serviceEndpoints <- ParsingUtils.parseServiceEndpoints(
              serviceEndpoints = ValueAtPath(value.serviceEndpoints, path / "serviceEndpoints"),
              serviceId = id,
              canBeEmpty = true,
              serviceEndpointCharLimit = serviceEndpointCharLenLimit
            )
            serviceTypeIsEmpty = serviceType.isEmpty
            serviceEndpointsIsEmpty = serviceEndpoints.isEmpty
            _ <-
              if (serviceTypeIsEmpty && serviceEndpointsIsEmpty) {
                val typePath = path / "type"
                val serviceEndpointsPath = path / "serviceEndpoints"
                Left(
                  MissingAtLeastOneValue(NonEmptyList(typePath, List(serviceEndpointsPath)))
                )
              } else Right(())
          } yield UpdateServiceAction(
            id,
            if (serviceTypeIsEmpty) None else Some(serviceType),
            if (serviceEndpointsIsEmpty) None else Some(serviceEndpoints)
          )

        case Action.PatchContext(value) =>
          val path = action.path / "patchContext"

          ParsingUtils
            .parseContext(ValueAtPath(value.context.toList, path / "context"), contextStringCharLimit)
            .map(PatchContextAction)

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
    val operationDigest = Sha256Hash.compute(operation.toByteArray)
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
