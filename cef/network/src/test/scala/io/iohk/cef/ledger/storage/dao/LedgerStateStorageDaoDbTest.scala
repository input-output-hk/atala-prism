package io.iohk.cef.ledger.storage.dao

import akka.util.ByteString
import io.iohk.cef.ledger.storage.scalike.{LedgerStateTable, LedgerStateTableEntry}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

trait LedgerStateStorageDaoDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers
  with MockitoSugar {

  private def insertPairs(ledgerId: Int, pairs: Seq[(String, ByteString)])(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    pairs.foreach{ case (key, serializedValue) =>
      sql"""insert into ${LedgerStateTable.table} (${column.ledgerStateId}, ${column.partitionId}, ${column.data})
            values (${ledgerId}, ${key}, ${serializedValue})
         """
    }
  }

  behavior of "LedgerStateStorageDao"

  it should "retrieve an slice" in { implicit session =>
  }
}
