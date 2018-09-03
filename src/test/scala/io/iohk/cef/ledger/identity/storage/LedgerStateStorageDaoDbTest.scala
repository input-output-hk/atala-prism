package io.iohk.cef.ledger.identity.storage

import akka.util.ByteString
import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc.scalatest.AutoRollback

trait LedgerStateStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with RSAKeyGenerator
    with OptionValues
    with IdentityLedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute an slice" in { implicit session =>
    val keys = (1 to 4).map(_ => generateKeyPair._1)
    val list = List(
      ("one", keys(0)),
      ("two", keys(1))
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one"))
    state.keys mustBe Set(keys(0))
    state.get("one").value mustBe Set(keys(0))
    state.get("two") mustBe None
  }

  it should "update a state" in { implicit session =>
    val keys = (1 to 5).map(_ => generateKeyPair._1)
    val list = List(
      ("zero", keys(0)),
      ("zero", keys(1)),
      ("one", keys(2)),
      ("two", keys(3))
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one", "zero"))
    val newState =
      new IdentityLedgerState(Map(("one", Set(keys(3))), ("three", Set(keys(4)))))
    storage.update(state, newState)
    val editedState = storage.slice(Set("one", "two", "three", "zero"))
    editedState.keys mustBe Set("one", "two", "three")
    Set("one", "two", "three").foreach(n => editedState.get(n).value mustBe Set(ByteString(n)))
  }
}
