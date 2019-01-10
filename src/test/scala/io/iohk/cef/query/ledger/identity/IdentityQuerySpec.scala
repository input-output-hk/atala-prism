package io.iohk.cef.query.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.identity.IdentityData
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.query.Query
import io.iohk.cef.query.ledger.LedgerQueryEngine
import io.iohk.cef.query.ledger.identity.IdentityQuery.{ExistsIdentity, RetrieveIdentityKeys}
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
}
