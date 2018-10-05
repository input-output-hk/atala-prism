package io.iohk.cef.ledger

import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

class BlockSpec extends FlatSpec with MustMatchers with EitherValues {

  behavior of "Block"

  val txs = List(
    DummyTransaction(1),
    DummyTransaction(2),
    DummyTransaction(20)
  )

  val header = DummyBlockHeader(txs.map(_.size).sum)
  val block = Block(header, txs)

  it should "apply itself to a state" in {
    val state = new LedgerState[String](Map())

    block(state).right.value mustBe LedgerState(Map())
  }

  it should "calculate keys correctly" in {
    block.partitionIds mustBe Set()
  }
}
