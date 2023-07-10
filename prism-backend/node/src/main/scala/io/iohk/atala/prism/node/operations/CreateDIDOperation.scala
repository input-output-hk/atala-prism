package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.KeyUsage.MasterKey
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, ProtocolConstants}
import io.iohk.atala.prism.node.operations.StateError.{EntityExists, InvalidKeyUsed, UnknownKey}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, ServicesDAO, ContextDAO}
import io.iohk.atala.prism.protos.{node_models => proto}

case class CreateDIDOperation(
    id: DidSuffix,
    keys: List[DIDPublicKey],
    services: List[DIDService],
    context: List[String],
    digest: Sha256Digest,
    ledgerData: LedgerData
) extends Operation {
  val metricCounterName: String = CreateDIDOperation.metricCounterName

  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    val keyOpt = keys.find(_.keyId == keyId)
    for {
      _ <- EitherT.fromEither[ConnectionIO] {
        keyOpt
          .filter(_.keyUsage == MasterKey)
          .toRight(InvalidKeyUsed("master key"))
      }
      data <- EitherT.fromEither[ConnectionIO] {
        keyOpt
          .map(didKey => CorrectnessData(didKey.key, None))
          .toRight(UnknownKey(id, keyId): StateError)
      }
    } yield data
  }

  override def applyStateImpl(_config: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] = {
    // type lambda T => EitherT[ConnectionIO, StateError, T]
    // in .traverse we need to express what Monad is to be used
    // as EitherT has 3 type parameters, it cannot be deduced from the context
    // we need to create a way to construct the Monad from the underlying type T
    type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

    for {
      _ <- EitherT {
        DIDDataDAO.insert(id, digest, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
          EntityExists("DID", id.getValue): StateError
        }
      }

      _ <- keys.traverse[ConnectionIOEitherTError, Unit] { key: DIDPublicKey =>
        EitherT {
          PublicKeysDAO.insert(key, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("public key", key.keyId): StateError
          }
        }
      }

      _ <- services.traverse[ConnectionIOEitherTError, Unit] { service: DIDService =>
        EitherT {
          ServicesDAO.insert(service, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("service", s"${service.didSuffix.getValue} - ${service.id}"): StateError
          }
        }
      }

      _ <- context.traverse[ConnectionIOEitherTError, Unit] { contextStr: String =>
        EitherT {
          ContextDAO.insert(contextStr, id, ledgerData).attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("context string", s"${id.getValue} - $contextStr"): StateError
          }
        }
      }

    } yield ()
  }
}

object CreateDIDOperation extends SimpleOperationCompanion[CreateDIDOperation] {
  val metricCounterName: String = "number_of_created_dids"

  def parseKeysFromData(
      data: ValueAtPath[proto.CreateDIDOperation.DIDCreationData],
      didSuffix: DidSuffix
  ): Either[ValidationError, List[DIDPublicKey]] = {
    val keysValue = data.child(_.publicKeys, "publicKeys")
    for {
      reversedKeys <- keysValue { keys =>
        keys.zipWithIndex.foldLeft(
          Either.right[ValidationError, List[DIDPublicKey]](List.empty)
        ) { (acc, keyi) =>
          val (key, i) = keyi
          acc.flatMap(list =>
            ParsingUtils
              .parseKey(
                ValueAtPath(key, keysValue.path / i.toString),
                didSuffix
              )
              .map(_ :: list)
          )
        }
      }
      _ <- Either.cond(
        reversedKeys.exists(_.keyUsage == MasterKey),
        (),
        keysValue.invalid("At least one master key has to be among CreateDID public keys")
      )
    } yield reversedKeys.reverse
  }

  def parseServicesFromData(
      data: ValueAtPath[proto.CreateDIDOperation.DIDCreationData],
      didSuffix: DidSuffix,
      servicesLimit: Int,
      serviceEndpointCharLimit: Int,
      serviceTypeCharLimit: Int
  ): Either[ValidationError, List[DIDService]] = {
    val servicesValue = data.child(_.services, "services")
    val services = servicesValue(identity)
    for {
      _ <- Either.cond(
        services.size < servicesLimit,
        (),
        servicesValue.invalid(
          s"Exceeded number of services while creating a DID, max - $servicesLimit, got - ${services.size}"
        )
      )
      eitherErrOrServices <- servicesValue { services =>
        type EitherValidationError[B] = Either[ValidationError, B]

        services.zipWithIndex.toList
          .traverse[EitherValidationError, DIDService] { case (service, index) =>
            ParsingUtils
              .parseService(
                ValueAtPath(service, servicesValue.path / index.toString),
                didSuffix,
                serviceEndpointCharLimit,
                serviceTypeCharLimit
              )
          }

      }
    } yield eitherErrOrServices

  }

  override def parse(
      operation: proto.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, CreateDIDOperation] = {

    val servicesLimit = ProtocolConstants.servicesLimit
    val serviceEndpointCharLenLimit = ProtocolConstants.serviceEndpointCharLenLimit
    val serviceTypeCharLimit = ProtocolConstants.serviceTypeCharLimit
    val contextStringCharLimit = ProtocolConstants.contextStringCharLimit

    val operationDigest = Sha256.compute(operation.toByteArray)
    val didSuffix = DidSuffix(operationDigest.getHexValue)
    val createOperation =
      ValueAtPath(operation, Path.root).child(_.getCreateDid, "createDid")
    for {
      data <- createOperation.childGet(_.didData, "didData")
      keys <- parseKeysFromData(data, didSuffix)
      services <- parseServicesFromData(
        data,
        didSuffix,
        servicesLimit,
        serviceEndpointCharLenLimit,
        serviceTypeCharLimit
      )
      context <- ParsingUtils.parseContext(data.child(_.context.toList, "context"), contextStringCharLimit)
    } yield CreateDIDOperation(didSuffix, keys, services, context, operationDigest, ledgerData)
  }
}
