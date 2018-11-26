package io.iohk.cef.ledger.storage.mv

import java.nio.file.Path
import java.util.UUID

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, Transaction}
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils.mv.MVTables

import scala.collection.JavaConverters._

class MVLedgerStorage(storageFile: Path) extends LedgerStorage {

  val mvTables = new MVTables(storageFile)

  override def push[S, Tx <: Transaction[S]](ledgerId: LedgerId, block: Block[S, Tx])(
      implicit codec: NioEncDec[Block[S, Tx]]): Unit =
    mvTables.table[Block[S, Tx]](ledgerId, codec).put(UUID.randomUUID().toString, block)

  def values[T](ledgerId: LedgerId, codec: NioEncDec[T]): Iterable[T] =
    mvTables.table[T](ledgerId, codec).values().asScala
}
