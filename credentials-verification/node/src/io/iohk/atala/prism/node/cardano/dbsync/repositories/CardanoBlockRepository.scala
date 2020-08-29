package io.iohk.atala.prism.node.cardano.dbsync.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureOptionOps
import io.iohk.atala.prism.node.cardano.dbsync.repositories.daos.{BlockDAO, TransactionDAO}
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}

import scala.concurrent.ExecutionContext

class CardanoBlockRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getFullBlock(blockNo: Int): FutureEither[BlockError.NotFound, Block.Full] = {
    val query = for {
      header <- BlockDAO.find(blockNo)
      transactions <- TransactionDAO.find(blockNo)
    } yield header.map(Block.Full(_, transactions))

    query
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(blockNo))
  }

  def getLatestBlock(): FutureEither[BlockError.NoneAvailable.type, Block.Canonical] = {
    BlockDAO
      .latest()
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NoneAvailable)
      .map(Block.Canonical)
  }
}
