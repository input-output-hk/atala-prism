package io.iohk.cef.ledger.storage.mv

import java.nio.file.Path
import java.util.UUID

import io.iohk.cef.utils.mv.MVTable

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, Transaction}
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.codecs.nio._

import scala.collection.JavaConverters._

class MVLedgerStorage[S, Tx <: Transaction[S]](ledgerId: LedgerId, storageFile: Path)(
    implicit codec: NioCodec[Block[S, Tx]]
) extends LedgerStorage[S, Tx](ledgerId) {

  val mvTable = new MVTable[Block[S, Tx]](ledgerId, storageFile, codec)

  override def push(block: Block[S, Tx]): Unit =
    mvTable.table.put(UUID.randomUUID().toString, block)

  def values: Iterable[Block[S, Tx]] =
    mvTable.table.values().asScala

  override def toString: LedgerId =
    s"${values.mkString("\n")}"
}
