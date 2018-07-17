package io.iohk.cef.ledger

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.ledger.identity._
import org.scalatest.{FlatSpec, MustMatchers}

class BlockSpec extends FlatSpec with MustMatchers {

  behavior of "Block"

  it should "apply itself to an state" in {
    val txs = List (Claim("one", ByteString("one")),
      Link("one", ByteString("two")),
      Unlink("one", ByteString("two")),
      Unlink("one", ByteString("one")))
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now)
    val block = Block(header, txs)
    val state = new IdentityLedgerState(Map())
    val newState = block(state)
    newState mustBe Right(IdentityLedgerState(Map()))
    val badTxs = List (Claim("one", ByteString("one")),
      Link("two", ByteString("two")),
      Unlink("one", ByteString("two")),
      Unlink("one", ByteString("one")))
    val newBlock = block.copy(transactions = badTxs)
    newBlock(state) mustBe Left(IdentityNotClaimedError("two"))
  }

  it should "calculate keys correctly" in {
    val txs = List (Claim("one", ByteString("one")),
      Link("two", ByteString("two")),
      Unlink("three", ByteString("two")),
      Unlink("three", ByteString("one")))
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now)
    val block = Block(header, txs)
    block.keys mustBe Set("one", "two", "three")
  }
}
