package io.iohk.cef.ledger.identity.storage
import akka.util.ByteString
import io.iohk.cef.ledger.identity.{IdentityLedgerState, IdentityLedgerStateStorageFixture}
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

trait IdentityLedgerStateStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with IdentityLedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute a slice" in { implicit session =>
    val list = List(
      ("one", ByteString("one")),
      ("two", ByteString("two"))
    )
    insertPairs(list)
    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one"))
    state.keys mustBe Set("one")
    state.get("one") mustBe Some(Set(ByteString("one")))
    state.get("two") mustBe None
  }

  it should "update a state" in { implicit session =>
    val list = List(
      ("zero", ByteString("zero")),
      ("zero", ByteString("zeroh")),
      ("one", ByteString("one")),
      ("two", ByteString("two"))
    )
    insertPairs(list)
    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one", "zero"))
    val newState =
      new IdentityLedgerState(Map(("one", Set(ByteString("one"))), ("three", Set(ByteString("three")))))
    storage.update(state, newState)
    val editedState = storage.slice(Set("one", "two", "three", "zero"))
    editedState.keys mustBe Set("one", "two", "three")
    Set("one", "two", "three").foreach(n => editedState.get(n) mustBe Some(Set(ByteString(n))))
  }
}
