package io.iohk.atala.prism.node.repositories.blocks

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Read}
import io.iohk.atala.prism.utils.DoobieImplicits._
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherOps, FutureOptionOps}
import io.iohk.atala.prism.node.bitcoin.models.{BlockError, BlockHeader, Blockhash}

import scala.concurrent.ExecutionContext

class BlocksRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  import BlocksRepository._

  def create(block: BlockHeader): FutureEither[Nothing, Unit] = {
    sql"""
         |INSERT INTO blocks (blockhash, height, time, previous_blockhash)
         |VALUES (${block.hash.value}, ${block.height}, ${block.time}, ${block.previous.map(_.value)})
     """.stripMargin.update.run
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(()))
      .toFutureEither
  }

  def find(blockhash: Blockhash): FutureEither[BlockError.NotFound, BlockHeader] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |WHERE blockhash = ${blockhash.value}
       """.stripMargin.query[BlockHeader].option

    program
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(BlockError.NotFound(blockhash))
  }

  def getLatest: FutureEither[BlockError.NoneAvailable.type, BlockHeader] = {
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
      .toFutureEither(BlockError.NoneAvailable)
  }

  def removeLatest(): FutureEither[BlockError.NoneAvailable.type, BlockHeader] = {
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
      .toFutureEither(BlockError.NoneAvailable)
  }
}

object BlocksRepository {
  implicit val blockhashGet: Get[Blockhash] = Get[List[Byte]].tmap { bytes =>
    Blockhash
      .from(bytes)
      .getOrElse(throw new RuntimeException("Corrupted blockhash"))
  }

  implicit val blockRead: Read[BlockHeader] = {
    Read[(Blockhash, Int, Long, Option[Blockhash])]
      .map((BlockHeader.apply _).tupled)
  }
}
