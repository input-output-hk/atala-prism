package io.iohk.node.cardano.dbsync.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureOptionOps
import io.iohk.node.cardano.models.{Block, BlockError, BlockHash}
import io.iohk.node.cardano.dbsync.repositories.daos.{BlockDAO, TransactionDAO}

import scala.concurrent.ExecutionContext

class CardanoBlockRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getBlock(hash: BlockHash): FutureEither[BlockError.NotFound, Block.Canonical] = {
    BlockDAO
      .find(hash)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(hash))
      .map(Block.Canonical)
  }

  def getFullBlock(hash: BlockHash): FutureEither[BlockError.NotFound, Block.Full] = {
    val query = for {
      header <- BlockDAO.find(hash)
      transactions <- TransactionDAO.find(hash)
    } yield header.map(Block.Full(_, transactions))

    query
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(hash))
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
