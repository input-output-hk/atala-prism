package io.iohk.cef.ledger.identity.storage

import akka.util.ByteString
import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc.scalatest.AutoRollback

trait LedgerStateStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with SigningKeyPairs
    with OptionValues
    with IdentityLedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute an slice" in { implicit session =>
    val list = List(
      ("one", alice.public),
      ("two", bob.public)
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one"))
    state.keys mustBe Set(alice.public)
    state.get("one").value mustBe Set(alice.public)
    state.get("two") mustBe None
  }

  it should "update a state" in { implicit session =>
    val list = List(
      ("zero", alice.public),
      ("zero", bob.public),
      ("one", carlos.public),
      ("two", daniel.public)
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one", "zero"))
    val newState =
      new IdentityLedgerState(Map(("one", Set(daniel.public)), ("three", Set(elena.public))))
    storage.update(state, newState)
    val editedState = storage.slice(Set("one", "two", "three", "zero"))
    editedState.keys mustBe Set("one", "two", "three")
    Set("one", "two", "three").foreach(n => editedState.get(n).value mustBe Set(ByteString(n)))
  }
}
