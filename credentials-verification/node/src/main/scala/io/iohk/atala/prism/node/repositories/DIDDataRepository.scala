package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.{DIDDataState, DIDPublicKeyState}
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, DIDSuffix}
import io.iohk.atala.prism.node.operations.TimestampInfo
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}

import scala.concurrent.ExecutionContext

class DIDDataRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  /** Creates DID record in the database
    *
    * @param didData did document information
    * @param timestampInfo the protocol timestamp information derived from the blockchain and sequence numbers
    * @return unit indicating success or error
    */
  def create(
      didData: DIDData,
      timestampInfo: TimestampInfo
  ): FutureEither[NodeError, Unit] = {
    val query = for {
      _ <- DIDDataDAO.insert(didData.didSuffix, didData.lastOperation)
      _ <- didData.keys.traverse((key: DIDPublicKey) => PublicKeysDAO.insert(key, timestampInfo))
    } yield ()

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def findByDidSuffix(didSuffix: DIDSuffix): FutureEither[NodeError, DIDDataState] = {
    val query = for {
      lastOperation <- OptionT(DIDDataDAO.getLastOperation(didSuffix))
        .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.suffix))
      keys <- EitherT.right[NodeError](PublicKeysDAO.findAll(didSuffix))
    } yield DIDDataState(didSuffix, keys, lastOperation)

    query
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def findKey(didSuffix: DIDSuffix, keyId: String): FutureEither[NodeError, DIDPublicKeyState] = {
    OptionT(PublicKeysDAO.find(didSuffix, keyId))
      .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.suffix))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
