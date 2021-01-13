package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.{DIDDataState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey}
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}

import scala.concurrent.ExecutionContext

class DIDDataRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  /** Creates DID record in the database
    *
    * @param didData did document information
    * @param ledgerData the information derived from the blockchain that carried the CreateDID operation
    * @return unit indicating success or error
    */
  def create(
      didData: DIDData,
      ledgerData: LedgerData
  ): FutureEither[NodeError, Unit] = {
    val query = for {
      _ <- DIDDataDAO.insert(didData.didSuffix, didData.lastOperation, ledgerData)
      _ <- didData.keys.traverse((key: DIDPublicKey) => PublicKeysDAO.insert(key, ledgerData))
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
        .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.value))
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
      .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.value))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
