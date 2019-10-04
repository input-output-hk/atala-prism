package io.iohk.node.repositories.blocks

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Read}
import io.iohk.node.bitcoin.models.{BlockError, BlockHeader, Blockhash}
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.{FutureEitherOps, FutureOptionOps}

import scala.concurrent.ExecutionContext

class BlocksRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  import BlocksRepository._

  def create(block: BlockHeader): FutureEither[Nothing, Unit] = {
    sql"""
         |INSERT INTO blocks (blockhash, height, time, previous_blockhash)
         |VALUES (${block.hash.toBytesBE}, ${block.height}, ${block.time}, ${block.previous.map(_.toBytesBE)})
     """.stripMargin.update.run
      .transact(xa)
      .map(_ => ())
      .unsafeToFuture()
      .map(Right.apply)
      .toFutureEither
  }

  def find(blockhash: Blockhash): FutureEither[BlockError.NotFound, BlockHeader] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |WHERE blockhash = ${blockhash.toBytesBE}
       """.stripMargin.query[BlockHeader].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(blockhash))
      .value
      .toFutureEither
  }

  def getLatest: FutureEither[BlockError.NoOneAvailable.type, BlockHeader] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |ORDER BY height DESC
           |LIMIT 1
       """.stripMargin.query[BlockHeader].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NoOneAvailable)
      .value
      .toFutureEither
  }

  def removeLatest(): FutureEither[BlockError.NoOneAvailable.type, BlockHeader] = {
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
       """.stripMargin.query[BlockHeader].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NoOneAvailable)
      .value
      .toFutureEither
  }
}

object BlocksRepository {
  private implicit val blockhashGet: Get[Blockhash] = Get[Array[Byte]].tmap { bytes =>
    Blockhash
      .fromBytesBE(bytes)
      .getOrElse(throw new RuntimeException("Corrupted blockhash"))
  }

  private implicit val blockRead: Read[BlockHeader] = {
    Read[(Blockhash, Int, Long, Option[Blockhash])]
      .map((BlockHeader.apply _).tupled)
  }
}
