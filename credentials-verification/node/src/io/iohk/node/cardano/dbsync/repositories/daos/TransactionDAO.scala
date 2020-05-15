package io.iohk.node.cardano.dbsync.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.utils.DoobieImplicits._
import io.iohk.node.cardano.models.{BlockHash, Transaction}

private[repositories] object TransactionDAO {
  def find(blockHash: BlockHash): ConnectionIO[List[Transaction]] = {
    // TODO: Sort transactions by their index within the block once possible, see
    //       https://github.com/input-output-hk/cardano-db-sync/issues/91
    sql"""
         |SELECT tx.hash, block.hash
         |FROM tx
         |JOIN block ON block.id = tx.block
         |WHERE block.hash = ${blockHash.value}
         |ORDER BY tx.id
       """.stripMargin.query[Transaction].to[List]
  }
}
