package io.iohk.atala.prism.node.cardano.dbsync.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.node.cardano.models.Transaction

private[repositories] object TransactionDAO {
  def find(blockNo: Int): ConnectionIO[List[Transaction]] = {
    sql"""
         |SELECT tx.hash, block.hash
         |FROM tx
         |JOIN block ON block.id = tx.block
         |WHERE block.block_no = $blockNo
         |ORDER BY tx.block_index
       """.stripMargin.query[Transaction].to[List]
  }
}
