package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.node.models.KeyUsage.MasterKey
import io.iohk.atala.prism.node.models.DIDPublicKey
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.StateError.{EntityExists, InvalidKeyUsed, UnknownKey}
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.{node_models => proto}

case class CreateDIDOperation(
    id: DIDSuffix,
    keys: List[DIDPublicKey],
    digest: SHA256Digest,
    ledgerData: LedgerData
) extends Operation {

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] = {
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

  override def applyState(): EitherT[ConnectionIO, StateError, Unit] = {
    // type lambda T => EitherT[ConnectionIO, StateError, T]
    // in .traverse we need to express what Monad is to be used
    // as EitherT has 3 type parameters, it cannot be deduced from the context
    // we need to create a way to construct the Monad from the underlying type T
    type ConnectionIOEitherTError[T] = EitherT[ConnectionIO, StateError, T]

    for {
      _ <- EitherT {
        DIDDataDAO.insert(id, digest, ledgerData).attemptSomeSqlState {
          case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("DID", id.getValue): StateError
        }
      }

      _ <- keys.traverse[ConnectionIOEitherTError, Unit] { key: DIDPublicKey =>
        EitherT {
          PublicKeysDAO.insert(key, ledgerData).attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              EntityExists("public key", key.keyId): StateError
          }
        }
      }
    } yield ()
  }
}

object CreateDIDOperation extends SimpleOperationCompanion[CreateDIDOperation] {

  def parseData(data: ValueAtPath[proto.DIDData], didSuffix: DIDSuffix): Either[ValidationError, List[DIDPublicKey]] = {
    for {
      _ <-
        data
          .child(_.id, "id")
          .parse { id =>
            Either.cond(id.isEmpty, (), "Id must be empty for DID creation operation")
          }
          .asInstanceOf[Either[ValidationError, Unit]]

      keysValue = data.child(_.publicKeys, "publicKeys")

      reversedKeys <- keysValue { keys =>
        keys.zipWithIndex.foldLeft(Either.right[ValidationError, List[DIDPublicKey]](List.empty)) { (acc, keyi) =>
          val (key, i) = keyi
          acc.flatMap(list =>
            ParsingUtils.parseKey(ValueAtPath(key, keysValue.path / i.toString), didSuffix).map(_ :: list)
          )
        }
      }
    } yield reversedKeys.reverse
  }

  override def parse(
      operation: proto.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, CreateDIDOperation] = {
    val operationDigest = SHA256Digest.compute(operation.toByteArray)
    val didSuffix = DIDSuffix.fromDigest(operationDigest)
    val createOperation = ValueAtPath(operation, Path.root).child(_.getCreateDid, "createDid")
    for {
      data <- createOperation.childGet(_.didData, "didData")
      keys <- parseData(data, didSuffix)
    } yield CreateDIDOperation(didSuffix, keys, operationDigest, ledgerData)
  }
}
