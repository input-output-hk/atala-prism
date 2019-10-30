package io.iohk.node.repositories.atalaobjects

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.Put
import io.iohk.node.bitcoin.models._
import io.iohk.node.services.models._
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class AtalaObjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  import AtalaObjectsRepository._

  def createReference(
      atalaObjectId: AtalaObjectId,
      bitcoinTxId: TransactionId
  ): FutureEither[Nothing, Unit] =
    sql"""  |
            | INSERT INTO atala_objects (atala_object_id, bitcoin_txid)
            | VALUES ($atalaObjectId, $bitcoinTxId)
            |
            |""".stripMargin.run

  private implicit class QueryExtensions(val query: doobie.util.fragment.Fragment) {
    def run: FutureEither[Nothing, Unit] =
      query.update.run
        .transact(xa)
        .map(_ => ())
        .unsafeToFuture()
        .map(Right.apply)
        .toFutureEither
  }

}

object AtalaObjectsRepository {
  private implicit val blockhashPut: Put[Blockhash] = Put[List[Byte]].contramap(_.toBytesBE)
  private implicit val transactionIdPut: Put[TransactionId] = Put[List[Byte]].contramap(_.toBytesBE)
  private implicit val atalaObjectIdPut: Put[AtalaObjectId] = Put[Array[Byte]].contramap(identity)
}
