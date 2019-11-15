package io.iohk.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.node.errors.NodeError
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.{DIDData, DIDPublicKey, DIDSuffix}
import io.iohk.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}

import scala.concurrent.ExecutionContext

class DIDDataRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  /** Creates DID record in the database
    *
    * @param didSuffix method-specific part of the DID (did:[method-name]:[did-suffix])
    * @param lastOperation hash of the operation affecting the record (creation here)
    * @param keys list of keys to be associated
    * @return unit indicating success or error
    */
  def create(
      didSuffix: DIDSuffix,
      lastOperation: Array[Byte],
      keys: List[DIDPublicKey]
  ): FutureEither[NodeError, Unit] = {
    val query = for {
      _ <- DIDDataDAO.insert(didSuffix, lastOperation)
      _ <- keys.traverse((key: DIDPublicKey) => PublicKeysDAO.insert(key))
    } yield ()

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def findByDidSuffix(didSuffix: DIDSuffix): FutureEither[NodeError, DIDData] = {
    val query = for {
      _ <- OptionT(DIDDataDAO.findByDidSuffix(didSuffix))
        .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.suffix))
      keys <- EitherT.right[NodeError](PublicKeysDAO.findAll(didSuffix))
    } yield DIDData(didSuffix, keys)

    query
      .transact(xa)
      .value
      .unsafeToFuture
      .toFutureEither
  }

  def findKey(didSuffix: DIDSuffix, keyId: String): FutureEither[NodeError, DIDPublicKey] = {
    OptionT(PublicKeysDAO.find(didSuffix, keyId))
      .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.suffix))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
