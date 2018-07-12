package io.iohk.cef.ledger.identity.storage
import akka.util.ByteString
import io.iohk.cef.db.AutoRollbackSpec
import io.iohk.cef.ledger.identity.IdentityLedgerStateImpl
import io.iohk.cef.ledger.identity.storage.db.IdentityLedgerStateTable
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._

import scala.concurrent.Future

class LedgerStateStorageImplSpec extends fixture.FlatSpec
  with AutoRollbackSpec
  with MustMatchers
  with MockFactory {

  def createStorage(session: DBSession) = new LedgerStateStorageImpl {
    override def createDb: DB = null

    override def inTx[T](db: DB)(block: DBSession => T): T = block(session)

    override def readOnly[T](db: DB)(block: DBSession => T): T = block(session)

    override def begin[T](f: DB => Future[T]): Future[T] = f(createDb)
  }

  def insertPairs(pairs: List[(String, ByteString)])(implicit session: DBSession) = {
    val column = IdentityLedgerStateTable.column
    pairs.foreach(item => {
      val (identity, publicKey) = item
      sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${publicKey.toArray})
            """.executeUpdate.apply()
    })
  }

  behavior of "LedgerStateStorage"

  it should "execute a slice" in { session =>
    implicit val s = session
    val list = List(
      ("one", ByteString("one")),
      ("two", ByteString("two"))
    )
    insertPairs(list)
    val storage = createStorage(session)
    val state = storage.slice(Set("one"))
    state.keys mustBe Set("one")
    state.get("one") mustBe Some(Set(ByteString("one")))
    state.get("two") mustBe None
  }

  it should "update a state" in { session =>
    implicit val s = session
    val list = List(
      ("zero", ByteString("zero")),
      ("zero", ByteString("zeroh")),
      ("one", ByteString("one")),
      ("two", ByteString("two"))
    )
    insertPairs(list)
    val storage = createStorage(session)
    val hash = storage.slice(Set("one", "zero")).hash
    val newState =
      new IdentityLedgerStateImpl(Map(("one", Set(ByteString("one"))),
        ("three", Set(ByteString("three"))),
        ("zero", Set())))
    storage.update(hash, newState)
    val editedState = storage.slice(Set("one", "two", "three"))
    editedState.keys mustBe Set("one", "two", "three")
    Set("one", "two", "three").foreach(n => editedState.get(n) mustBe Some(Set(ByteString(n))))
  }
}
