package io.iohk.node.repositories.blocks

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Read}
import io.iohk.node.bitcoin.models.{Block, Blockhash}

import scala.concurrent.Future

class BlocksRepository(xa: Transactor[IO]) {

  import BlocksRepository._

  def create(block: Block): Future[Unit] = {
    sql"""
         |INSERT INTO blocks (blockhash, height, time, previous_blockhash)
         |VALUES (${block.hash.toBytesBE}, ${block.height}, ${block.time}, ${block.previous.map(_.toBytesBE)})
     """.stripMargin.update.run.transact(xa).map(_ => ()).unsafeToFuture()
  }

  def find(blockhash: Blockhash): Future[Option[Block]] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |WHERE blockhash = ${blockhash.toBytesBE}
       """.stripMargin.query[Block].option

    program.transact(xa).unsafeToFuture()
  }

  def getLatest: Future[Option[Block]] = {
    val program =
      sql"""
           |SELECT blockhash, height, time, previous_blockhash
           |FROM blocks
           |ORDER BY height DESC
           |LIMIT 1
       """.stripMargin.query[Block].option

    program.transact(xa).unsafeToFuture()
  }

  def removeLatest(): Future[Option[Block]] = {
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

    program.transact(xa).unsafeToFuture()
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
