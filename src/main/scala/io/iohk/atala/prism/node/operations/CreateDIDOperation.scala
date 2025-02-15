package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.KeyUsage.MasterKey
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, ProtocolConstants}
import io.iohk.atala.prism.node.operations.StateError.{EntityExists, IllegalSecp256k1Key, InvalidKeyUsed, UnknownKey}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{ContextDAO, DIDDataDAO, PublicKeysDAO, ServicesDAO}
import io.iohk.atala.prism.protos.{node_models => proto}

import scala.util.Try

case class CreateDIDOperation(
    id: DidSuffix,
    keys: List[DIDPublicKey],
    services: List[DIDService],
    context: List[String],
    digest: Sha256Hash,
    ledgerData: LedgerData
) extends Operation {
  val metricCounterName: String = CreateDIDOperation.metricCounterName

  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    val keyOpt = keys.find(_.keyId == keyId)
    for {
      key <- EitherT.fromEither[ConnectionIO] {
        keyOpt
          .filter(_.keyUsage == MasterKey)
          .toRight(InvalidKeyUsed("master key"))
      }
      secpKey <- EitherT.fromEither[ConnectionIO] {
        val tryKey = Try {
          SecpPublicKey.unsafeFromCompressed(key.key.compressedKey)
        }
        tryKey.toOption
          .toRight(IllegalSecp256k1Key(key.keyId))
      }
      data <- EitherT.fromEither[ConnectionIO] {
        keyOpt
          .map(_ => CorrectnessData(secpKey, None))
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
      didSuffix: DidSuffix,
      publicKeysLimit: Int,
      idCharLenLimit: Int
  ): Either[ValidationError, List[DIDPublicKey]] = {

    val keysValue = data.child(_.publicKeys, "publicKeys")
    val keys = keysValue(identity)
    for {
      _ <- Either.cond(
        keys.size <= publicKeysLimit,
        (),
        keysValue.invalid(
          s"Exceeded number of services while creating a DID, max - $publicKeysLimit, got - ${keys.size}"
        )
      )

      reversedKeys <- keysValue { keys =>
        keys.zipWithIndex.foldLeft(
          Either.right[ValidationError, List[DIDPublicKey]](List.empty)
        ) { (acc, keyi) =>
          val (key, i) = keyi
          acc.flatMap(list =>
            ParsingUtils
              .parseKey(
                ValueAtPath(key, keysValue.path / i.toString),
                didSuffix,
                idCharLenLimit
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
      serviceTypeCharLimit: Int,
      idCharLenLimit: Int
  ): Either[ValidationError, List[DIDService]] = {
    val servicesValue = data.child(_.services, "services")
    val services = servicesValue(identity)
    for {
      _ <- Either.cond(
        services.size <= servicesLimit,
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
                serviceTypeCharLimit,
                idCharLenLimit
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
    val publicKeysLimit = ProtocolConstants.publicKeysLimit
    val idCharLenLimit = ProtocolConstants.idCharLenLimit
    val serviceEndpointCharLenLimit = ProtocolConstants.serviceEndpointCharLenLimit
    val serviceTypeCharLimit = ProtocolConstants.serviceTypeCharLimit
    val contextStringCharLimit = ProtocolConstants.contextStringCharLimit

    val operationDigest = Sha256Hash.compute(operation.toByteArray)
    val didSuffix = DidSuffix(operationDigest.hexEncoded)
    val createOperation =
      ValueAtPath(operation, Path.root).child(_.getCreateDid, "createDid")
    for {
      data <- createOperation.childGet(_.didData, "didData")
      keys <- parseKeysFromData(data, didSuffix, publicKeysLimit, idCharLenLimit)
      services <- parseServicesFromData(
        data,
        didSuffix,
        servicesLimit,
        serviceEndpointCharLenLimit,
        serviceTypeCharLimit,
        idCharLenLimit
      )
      context <- ParsingUtils.parseContext(data.child(_.context.toList, "context"), contextStringCharLimit)
    } yield CreateDIDOperation(didSuffix, keys, services, context, operationDigest, ledgerData)
  }
}
