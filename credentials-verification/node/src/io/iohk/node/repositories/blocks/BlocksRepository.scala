package io.iohk.node.repositories.blocks

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Read}
import io.iohk.node.bitcoin.models.{Block, BlockError, Blockhash}
import io.iohk.node.utils.FutureEither.FutureOptionOps

import scala.concurrent.{ExecutionContext, Future}

class BlocksRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  import BlocksRepository._

  def create(block: Block): Future[Either[Nothing, Unit]] = {
    sql"""
         |INSERT INTO blocks (blockhash, height, time, previous_blockhash)
         |VALUES (${block.hash.toBytesBE}, ${block.height}, ${block.time}, ${block.previous.map(_.toBytesBE)})
     """.stripMargin.update.run
      .transact(xa)
      .map(_ => ())
      .unsafeToFuture()
      .map(Right.apply)
  }

  def find(blockhash: Blockhash): Future[Either[BlockError.NotFound, Block]] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |WHERE blockhash = ${blockhash.toBytesBE}
       """.stripMargin.query[Block].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(blockhash))
      .value
  }

  def getLatest: Future[Either[BlockError.NoOneAvailable.type, Block]] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |ORDER BY height DESC
           |LIMIT 1
       """.stripMargin.query[Block].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NoOneAvailable)
      .value
  }

  def removeLatest(): Future[Either[BlockError.NoOneAvailable.type, Block]] = {
    val program =
      sql"""
           |WITH CTE AS (
           |  SELECT blockhash AS latest_blockhash
           |  FROM blocks
           |  ORDER BY height DESC
           |  LIMIT 1
           |)
           |DELETE FROM blocks
           |USING CTE
           |WHERE blockhash = latest_blockhash
           |RETURNING blockhash, height, time, previous_blockhash
       """.stripMargin.query[Block].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NoOneAvailable)
      .value
  }
}

object BlocksRepository {
  private implicit val blockhashGet: Get[Blockhash] = Get[Array[Byte]].tmap { bytes =>
    Blockhash
      .fromBytesBE(bytes)
      .getOrElse(throw new RuntimeException("Corrupted blockhash"))
  }

  private implicit val blockRead: Read[Block] = {
    Read[(Blockhash, Int, Long, Option[Blockhash])]
      .map {
        case (blockhash, height, time, previous) =>
          Block(blockhash, height, time, previous)
      }
  }
}
