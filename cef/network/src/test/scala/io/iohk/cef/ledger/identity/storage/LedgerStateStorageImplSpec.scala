package io.iohk.cef.ledger.identity.storage
import akka.util.ByteString
import io.iohk.cef.db.AutoRollbackSpec
import io.iohk.cef.ledger.identity._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, fixture}

class LedgerStateStorageImplSpec extends fixture.FlatSpec
  with AutoRollbackSpec
  with MustMatchers
  with MockFactory
  with LedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute a slice" in { implicit session =>
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

  it should "update a state" in { implicit session =>
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
