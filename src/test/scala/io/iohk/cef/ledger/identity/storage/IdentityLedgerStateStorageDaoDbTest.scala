package io.iohk.cef.ledger.identity.storage

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.identity.{IdentityLedgerState, IdentityLedgerStateStorageFixture}
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc.scalatest.AutoRollback

trait IdentityLedgerStateStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with SigningKeyPairs
    with OptionValues
    with IdentityLedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute a slice" in { implicit session =>
    val list = List(
      ("one", alice.public),
      ("two", bob.public)
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one"))
    state.keys mustBe Set("one")
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
    val newState = new IdentityLedgerState(
      Map(
        ("one", Set(carlos.public)),
        ("three", Set(elena.public))
      ))
    storage.update(state, newState)

    val editedState = storage.slice(Set("one", "two", "three", "zero"))
    editedState.keys mustBe Set("one", "two", "three")

    editedState.get("one").value mustEqual Set(carlos.public)
    editedState.get("two").value mustEqual Set(daniel.public)
    editedState.get("three").value mustEqual Set(elena.public)
  }
}
