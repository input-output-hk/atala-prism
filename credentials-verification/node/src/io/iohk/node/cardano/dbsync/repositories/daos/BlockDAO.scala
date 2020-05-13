package io.iohk.node.cardano.dbsync.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.node.cardano.models.{BlockHash, BlockHeader}

private[repositories] object BlockDAO {
  def find(hash: BlockHash): ConnectionIO[Option[BlockHeader]] = {
    sql"""
           |SELECT b.hash, b.block_no, b.time, bprev.hash
           |FROM block AS b
           |LEFT JOIN block AS bprev ON b.previous = bprev.id
           |WHERE b.hash = ${hash.toBytesBE}
           |  AND b.block_no IS NOT NULL
       """.stripMargin.query[BlockHeader].option
  }

  def latest(): ConnectionIO[Option[BlockHeader]] = {
    sql"""
           |SELECT b.hash, b.block_no, b.time, bprev.hash
           |FROM block AS b
           |LEFT JOIN block AS bprev ON b.previous = bprev.id
           |WHERE b.block_no IS NOT NULL
           |ORDER BY b.block_no DESC
           |LIMIT 1
       """.stripMargin.query[BlockHeader].option
  }
}
