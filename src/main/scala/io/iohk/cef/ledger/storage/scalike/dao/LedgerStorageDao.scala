package io.iohk.cef.ledger.storage.scalike.dao

import java.time.{Clock, Instant}

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.scalike.LedgerTable
import scalikejdbc._

class LedgerStorageDao(clock: Clock) {

  def push[S, Header <: BlockHeader, Tx <: Transaction[S]](ledgerId: LedgerId, block: Block[S, Header, Tx])(
      implicit blockSerializable: ByteStringSerializable[Block[S, Header, Tx]],
      session: DBSession): Int = {
    val blockColumn = LedgerTable.column
    val lt = LedgerTable.syntax("bt")

    val serializedBlock = blockSerializable.serialize(block)
    val maxBlockNumber =
      sql"""select max(${blockColumn.blockNumber}) as max_block_number
            from ${LedgerTable as lt}
            where ${blockColumn.ledgerId} = ${ledgerId}"""
        .map(rs => rs.longOpt("max_block_number"))
        .single()
        .apply()
        .flatten
        .getOrElse(0L)
    val previousBlockId =
      sql"""select ${lt.result.id}
            from ${LedgerTable as lt}
            where ${blockColumn.ledgerId} = ${ledgerId} and ${blockColumn.blockNumber} = ${maxBlockNumber}
         """.map(_.longOpt(lt.resultName.id)).single.apply().flatten
    sql"""insert into cef.ledger_block (
        ${blockColumn.ledgerId},
        ${blockColumn.blockNumber},
        ${blockColumn.previousBlockId},
        ${blockColumn.createdOn},
        ${blockColumn.data})
      values (
        ${ledgerId},
        ${maxBlockNumber + 1},
        ${previousBlockId},
        ${Instant.now(clock)},
        ${serializedBlock.toArray})""".executeUpdate().apply()
  }
}
