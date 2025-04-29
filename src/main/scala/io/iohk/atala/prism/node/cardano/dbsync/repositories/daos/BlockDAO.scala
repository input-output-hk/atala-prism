package io.iohk.atala.prism.node.cardano.dbsync.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.log.LogHandler
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata
import io.iohk.atala.prism.node.cardano.models.BlockHeader
import io.iohk.atala.prism.node.cardano.models.Transaction
private[repositories] object BlockDAO {
  def find(blockNo: Int): ConnectionIO[Option[BlockHeader]] = {
    sql"""
         |SELECT b.hash, b.block_no, b.time, bprev.hash
         |FROM block AS b
         |LEFT JOIN block AS bprev ON b.previous_id = bprev.id
         |WHERE b.block_no = $blockNo
       """.stripMargin.query[BlockHeader].option
  }

  def latest(): ConnectionIO[Option[BlockHeader]] = {
    sql"""
           |SELECT b.hash, b.block_no, b.time, bprev.hash
           |FROM block AS b
           |LEFT JOIN block AS bprev ON b.previous_id = bprev.id
           |WHERE b.block_no IS NOT NULL
           |ORDER BY b.block_no DESC
           |LIMIT 1
       """.stripMargin.query[BlockHeader].option
  }

  def findAllPrismIndex(): ConnectionIO[List[(BlockHeader, List[Transaction])]] = {

    val query = sql"""
      WITH block_data AS (
        SELECT 
          b.hash as block_hash,
          b.block_no,
          b.time as block_time,
          tx.hash as tx_hash,
          tx.block_index,
          tx_metadata.key as metadata_key,
          tx_metadata.json as metadata_json
        FROM block b
        LEFT JOIN tx ON tx.block_id = b.id
        INNER JOIN tx_metadata ON tx_metadata.tx_id = tx.id 
          AND tx_metadata.key = ${AtalaObjectMetadata.METADATA_PRISM_INDEX}
        ORDER BY b.block_no, tx.block_index
      )
      SELECT * FROM block_data;
    """.stripMargin
    query
      .queryWithLogHandler[BlockTransactionData](LogHandler.jdkLogHandler)
      .to[List]
      .map { results =>
        results
          .groupBy(row =>
            BlockHeader(
              hash = row.blockHash,
              blockNo = row.blockNo,
              time = row.time,
              previousBlockHash = None // No previous block hash needed
            )
          )
          .map { case (header, rows) =>
            val transactions = rows
              .map(row =>
                Transaction(
                  id = row.id,
                  blockHash = row.blockHash,
                  blockIndex = row.blockIndex,
                  metadata = row.metadata
                )
              )
              .toList

            (header, transactions)
          }
          .toList
          .sortBy(_._1.blockNo)
      }
  }
}
