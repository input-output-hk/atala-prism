package io.iohk.cef.ledger.storage

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger._
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.{verify, when}

class LedgerSpec extends FlatSpec {

  private val ledgerStorage = mock[LedgerStorage[String, ExplicitTransaction]]
  private val ledgerStateStorage = mock[LedgerStateStorage[String]]

  behavior of "Ledger"

  it should "apply state changes correctly" in {
    // given
    val s0 = LedgerState[String](
      "A" -> "initial-value-for-A",
      "B" -> "initial-value-for-B"
    )
    when(ledgerStateStorage.slice(Set("A", "B"))).thenReturn(s0)

    val ledger = Ledger("ledger-id", ledgerStorage, ledgerStateStorage)

    val block = Block[String, ExplicitTransaction](
      BlockHeader(),
      Seq(
        Add("A", "new-value-for-A"),
        Remove("B")
      )
    )

    // when
    ledger.apply(block)

    // then
    verify(ledgerStateStorage).update(s0, LedgerState[String]("A" -> "new-value-for-A"))
  }
}

sealed trait ExplicitTransaction extends Transaction[String]

case class Add(partitionId: String, value: String) extends ExplicitTransaction {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] =
    Right(LedgerState(v1.map + (partitionId -> value)))

  override def partitionIds: Set[String] = Set(partitionId)

  override def toString(): String = s"Add($partitionId, $value)"
}

case class Remove(partitionId: String) extends ExplicitTransaction {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] =
    Right(LedgerState(v1.map - partitionId))

  override def partitionIds: Set[String] = Set(partitionId)

  override def toString(): String = s"Remove($partitionId)"
}

case class Update(partitionId: String, value: String) extends ExplicitTransaction {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] =
    Right(LedgerState(v1.map + (partitionId -> value)))

  override def partitionIds: Set[String] = Set(partitionId)

  override def toString(): String = s"Update($partitionId, $value)"
}
