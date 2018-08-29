package io.iohk.cef.ledger.identity.storage

import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.identity.{IdentityLedgerState, IdentityLedgerStateStorageFixture}
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc.scalatest.AutoRollback

trait IdentityLedgerStateStorageDaoDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers
  with RSAKeyGenerator
  with OptionValues
  with IdentityLedgerStateStorageFixture {

  behavior of "LedgerStateStorage"

  it should "execute a slice" in { implicit session =>
    val keys = (1 to 4).map(_ => generateKeyPair._1)
    val list = List(
      ("one", keys(0)),
      ("two", keys(1))
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one"))
    state.keys mustBe Set("one")
    state.get("one").value mustBe Set(keys(0))
    state.get("two") mustBe None
  }

  it should "update a state" in { implicit session =>
    val keys = (1 to 5).map(_ => generateKeyPair._1)
    val (zero, _) = generateKeyPair
    val (zeroh, _) = generateKeyPair
    val (one, _) = generateKeyPair
    val (two, _) = generateKeyPair
    val (three, _) = generateKeyPair
    val list = List(
      ("zero", zero),
      ("zero", zeroh),
      ("one", one),
      ("two", two)
    )
    insertPairs(list)

    val storage = new IdentityLedgerStateStorageDao
    val state = storage.slice(Set("one", "zero"))
    val newState = new IdentityLedgerState(Map(
      ("one", Set(one)),
      ("three", Set(three))
    ))
    storage.update(state, newState)

    val editedState = storage.slice(Set("one", "two", "three", "zero"))
    editedState.keys mustBe Set("one", "two", "three")

    editedState.get("one").value mustEqual Set(one)
    editedState.get("two").value mustEqual Set(two)
    editedState.get("three").value mustEqual Set(three)
  }
}
