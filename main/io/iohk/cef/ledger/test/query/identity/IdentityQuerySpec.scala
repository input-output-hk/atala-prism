package io.iohk.cef.ledger.query.identity

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.identity.IdentityData
import io.iohk.cef.ledger.query.LedgerQueryEngine
import io.iohk.cef.ledger.query.identity.IdentityQuery._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.query.Query
import io.iohk.crypto._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, MustMatchers}

class IdentityQuerySpec extends FlatSpec with MustMatchers {

  behavior of "IdentityQuery"

  it should "query for the identity keys" in {
    val stateStorage = mock[LedgerStateStorage[IdentityPartition]]
    val mockKey = mock[SigningPublicKey]
    val engine = LedgerQueryEngine(stateStorage)
    when(stateStorage.slice(Set("identity"))).thenReturn(LedgerState("identity" -> IdentityData.forKeys(mockKey)))
    when(stateStorage.slice(Set("a"))).thenReturn(LedgerState("identity" -> IdentityData.forKeys(mockKey)))

    val queryForIdentity = RetrieveIdentityKeys("identity")
    val queryForA = RetrieveIdentityKeys("a")
    Query.performer(queryForIdentity, engine) mustBe Set(mockKey)
    Query.performer(queryForA, engine) mustBe Set()
  }

  it should "query for the existence of an identity" in {
    val stateStorage = mock[LedgerStateStorage[IdentityPartition]]
    val mockKey = mock[SigningPublicKey]
    val engine = LedgerQueryEngine(stateStorage)
    when(stateStorage.slice(Set("identity"))).thenReturn(LedgerState("identity" -> IdentityData.forKeys(mockKey)))
    when(stateStorage.slice(Set("a"))).thenReturn(LedgerState("identity" -> IdentityData.forKeys(mockKey)))

    val queryForIdentity = ExistsIdentity("identity")
    val queryForA = ExistsIdentity("a")
    Query.performer(queryForIdentity, engine) mustBe true
    Query.performer(queryForA, engine) mustBe false
  }

  it should "query the identity endorsers" in {
    val endorsedIdentity = "x"
    val endorsedBy = Set("a", "b", "c")
    val identityWithoutEndorsements = "y"

    val stateStorage = mock[LedgerStateStorage[IdentityPartition]]
    def prepareState(key: String, data: IdentityData) = {
      when(stateStorage.slice(Set(key))).thenReturn(LedgerState(key -> data))
    }

    val engine = LedgerQueryEngine(stateStorage)
    prepareState(endorsedIdentity, IdentityData(endorsers = endorsedBy, keys = Set.empty))
    prepareState(identityWithoutEndorsements, IdentityData.empty)

    Query.performer(RetrieveEndorsers(endorsedIdentity), engine) mustBe endorsedBy
    Query.performer(RetrieveEndorsers(identityWithoutEndorsements), engine) mustBe empty
  }

  it should "query the identity endorsements" in {
    val identity = "x"
    val endorsements = Set("a", "c")

    val stateStorage = mock[LedgerStateStorage[IdentityPartition]]
    def prepareState(key: String, data: IdentityData) = {
      when(stateStorage.slice(Set(key))).thenReturn(LedgerState(key -> data))
    }
    when(stateStorage.keys).thenReturn(Set("x", "a", "b", "c"))

    val engine = LedgerQueryEngine(stateStorage)
    prepareState(identity, IdentityData.empty.copy(endorsers = Set("a")))
    prepareState("a", IdentityData.empty.copy(endorsers = Set(identity, "b")))
    prepareState("b", IdentityData.empty.copy(endorsers = Set("c", "a")))
    prepareState("c", IdentityData.empty.copy(endorsers = Set(identity)))

    Query.performer(RetrieveEndorsements(identity), engine) mustBe endorsements
    Query.performer(RetrieveEndorsements("b"), engine) mustBe Set("a")
  }

  it should "query the identities" in {
    val identities = Set("x", "a", "b", "c")

    val stateStorage = mock[LedgerStateStorage[IdentityPartition]]
    when(stateStorage.keys).thenReturn(identities)

    val engine = LedgerQueryEngine(stateStorage)

    Query.performer(RetrieveIdentities, engine) mustBe identities
  }
}
