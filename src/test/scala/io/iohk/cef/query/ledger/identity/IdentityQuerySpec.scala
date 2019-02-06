package io.iohk.cef.query.ledger.identity

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.identity.IdentityData
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.query.Query
import io.iohk.cef.query.ledger.LedgerQueryEngine
import io.iohk.cef.query.ledger.identity.IdentityQuery.{ExistsIdentity, RetrieveEndorsers, RetrieveIdentityKeys}
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
}
