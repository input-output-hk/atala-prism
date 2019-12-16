package io.iohk.node.operations

import java.security.PublicKey

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.models.{DIDPublicKey, DIDSuffix, KeyUsage, SHA256Digest}
import io.iohk.node.operations.OperationKey.IncludedKey
import io.iohk.node.operations.StateError.{EntityExists, UnknownKey}
import io.iohk.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.node.operations.path._
import io.iohk.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.nodenew.{geud_node_new => proto}

import scala.util.Try

case class CreateDIDOperation(id: DIDSuffix, keys: List[DIDPublicKey], digest: SHA256Digest) extends Operation {

  override def getKey(keyId: String): Either[StateError, OperationKey] = {
    keys
      .find(_.keyId == keyId)
      .map(didKey => IncludedKey(didKey.key))
      .toRight(UnknownKey(id, keyId))
  }

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
      _ <- EitherT {
        DIDDataDAO.insert(id, digest).attemptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("DID", id.suffix): StateError
        }
      }

      _ <- keys.traverse[ConnectionIOEitherTError, Unit] { key: DIDPublicKey =>
        EitherT {
          PublicKeysDAO.insert(key).attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              EntityExists("public key", key.keyId): StateError
          }
        }
      }
    } yield ()
  }
}

object CreateDIDOperation extends OperationCompanion[CreateDIDOperation] {
  val KEY_ID_RE = "^\\w+$".r

  def parseECKey(ecData: ValueAtPath[proto.ECKeyData]): Either[ValidationError, PublicKey] = {
    if (ecData(_.curve) != ECKeys.CURVE_NAME) {
      Either.left(ecData.child(_.curve, "curve").invalid("Unsupported curve"))
    } else if (ecData(_.x.toByteArray.isEmpty)) {
      Either.left(ecData.child(_.curve, "x").missing())
    } else if (ecData(_.y.toByteArray.isEmpty)) {
      Either.left(ecData.child(_.curve, "y").missing())
    } else {
      Try(ECKeys.toPublicKey(ecData(_.x.toByteArray), ecData(_.y.toByteArray))).toEither.left
        .map(ex => InvalidValue(ecData.path, "", s"Unable to initialize the key: ${ex.getMessage}"))
    }
  }

  def parseKey(key: ValueAtPath[proto.PublicKey], didSuffix: DIDSuffix): Either[ValidationError, DIDPublicKey] = {
    for {
      keyUsage <- key.child(_.usage, "usage").parse {
        case proto.KeyUsage.MASTER_KEY => Either.right(KeyUsage.MasterKey)
        case proto.KeyUsage.ISSUING_KEY => Either.right(KeyUsage.IssuingKey)
        case proto.KeyUsage.AUTHENTICATION_KEY => Either.right(KeyUsage.AuthenticationKey)
        case proto.KeyUsage.COMMUNICATION_KEY => Either.right(KeyUsage.CommunicationKey)
        case _ => Either.left("Unknown value")
      }
      keyId <- key.child(_.id, "id").parse { id =>
        Either.cond(KEY_ID_RE.pattern.matcher(id).matches(), id, "Invalid key id")
      }
      _ <- Either.cond(key(_.keyData.isDefined), (), MissingValue(key.path / "keyData"))
      publicKey <- parseECKey(key.child(_.getEcKeyData, "ecKeyData"))
    } yield DIDPublicKey(didSuffix, keyId, keyUsage, publicKey)
  }

  def parseData(data: ValueAtPath[proto.DIDData], didSuffix: DIDSuffix): Either[ValidationError, List[DIDPublicKey]] = {
    for {
      _ <- data
        .child(_.id, "id")
        .parse { id =>
          Either.cond(id.isEmpty, (), "Id must be empty for DID creation operation")
        }
        .asInstanceOf[Either[ValidationError, Unit]]

      keysValue = data.child(_.publicKeys, "publicKeys")

      reversedKeys <- keysValue { keys =>
        keys.zipWithIndex.foldLeft(Either.right[ValidationError, List[DIDPublicKey]](List.empty)) { (acc, keyi) =>
          val (key, i) = keyi
          acc.flatMap(list => parseKey(ValueAtPath(key, keysValue.path / i.toString), didSuffix).map(_ :: list))
        }
      }
    } yield reversedKeys.reverse
  }

  override def parse(operation: proto.AtalaOperation): Either[ValidationError, CreateDIDOperation] = {
    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val didSuffix = DIDSuffix(operationDigest)
    val createOperation = ValueAtPath(operation, Path.root).child(_.getCreateDid, "createDid")
    for {
      data <- createOperation.childGet(_.didData, "didData")
      keys <- parseData(data, didSuffix)
    } yield CreateDIDOperation(didSuffix, keys, operationDigest)
  }
}
