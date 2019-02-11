package io.iohk.cef.ledger.query

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.storage.LedgerStateStorage
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, MustMatchers}

class LedgerQueryEngineSpec extends FlatSpec with MustMatchers {

  behavior of "LedgerQueryEngine"

  it should "get a partition" in {
    val stateStorage = mock[LedgerStateStorage[String]]

    when(stateStorage.slice(Set("partition"))).thenReturn(LedgerState("partition" -> "thing"))
    when(stateStorage.slice(Set("a"))).thenReturn(LedgerState("partition" -> "thing"))
    val engine = LedgerQueryEngine(stateStorage)
    engine.get("a") mustBe None
    engine.get("partition") mustBe Some("thing")
  }

  it should "tell if a value is contained" in {
    val stateStorage = mock[LedgerStateStorage[String]]

    when(stateStorage.slice(Set("partition"))).thenReturn(LedgerState("partition" -> "thing"))
    when(stateStorage.slice(Set("b"))).thenReturn(LedgerState("partition" -> "thing"))
    val engine = LedgerQueryEngine(stateStorage)
    engine.contains("partition") mustBe true
    engine.contains("b") mustBe false
  }

}
