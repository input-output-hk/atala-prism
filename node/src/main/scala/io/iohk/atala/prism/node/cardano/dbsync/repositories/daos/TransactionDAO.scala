package io.iohk.atala.prism.node.cardano.dbsync.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.node.cardano.models.{AtalaObjectMetadata, Transaction}

private[repositories] object TransactionDAO {
  def find(blockNo: Int): ConnectionIO[List[Transaction]] = {
    sql"""
         |SELECT tx.hash, block.hash, tx.block_index, tx_metadata.key, tx_metadata.json
         |FROM tx
         |JOIN block ON block.id = tx.block_id
         |LEFT OUTER JOIN tx_metadata
         |  ON tx_metadata.tx_id = tx.id
         |    AND tx_metadata.key = ${AtalaObjectMetadata.METADATA_PRISM_INDEX}
         |WHERE block.block_no = $blockNo
         |ORDER BY tx.block_index
       """.stripMargin.query[Transaction].to[List]
  }
}
