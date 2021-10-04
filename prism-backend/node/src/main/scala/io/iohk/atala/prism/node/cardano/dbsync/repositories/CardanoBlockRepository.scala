package io.iohk.atala.prism.node.cardano.dbsync.repositories

import cats.effect.BracketThrow
import cats.syntax.either._
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.cardano.dbsync.repositories.daos.{BlockDAO, TransactionDAO}
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}
import org.slf4j.{Logger, LoggerFactory}

trait CardanoBlockRepository[F[_]] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]]
  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]]
}

object CardanoBlockRepository {
  def apply[F[_]: BracketThrow](xa: Transactor[F]): CardanoBlockRepository[F] = {
    new CardanoBlockRepositoryImpl(xa)
  }
}

private final class CardanoBlockRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends CardanoBlockRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]] = {
    val query = for {
      header <- BlockDAO.find(blockNo)
      transactions <- TransactionDAO.find(blockNo)
    } yield header.map(Block.Full(_, transactions))

    query
      .logSQLErrors("getting full block", logger)
      .transact(xa)
      .map(_.toRight(BlockError.NotFound(blockNo)))
  }

  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]] = {
    BlockDAO
      .latest()
      .logSQLErrors("getting latest block", logger)
      .transact(xa)
      .map(
        _.fold[Either[BlockError.NoneAvailable.type, Block.Canonical]](BlockError.NoneAvailable.asLeft)(header =>
          Block.Canonical(header).asRight
        )
      )
  }
}
