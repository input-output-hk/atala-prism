package io.iohk.cef.ledger

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.ledger.identity._
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

class BlockSpec
    extends FlatSpec
    with MustMatchers
    with RSAKeyGenerator
    with EitherValues {

  behavior of "Block"

  it should "apply itself to a state" in {
    val keys = (1 to 2).map(_ => generateKeyPair._1)

    val txs = List (Claim("one", keys(0)),
      Link("one", keys(1)),
      Unlink("one", keys(1)),
      Unlink("one", keys(0)))

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    val state = new IdentityLedgerState(Map())
    val newState = block(state)

    newState.right.value mustBe IdentityLedgerState(Map())

    val badTxs = List (Claim("one", keys(0)),
      Link("two", keys(1)),
      Unlink("one", keys(1)),
      Unlink("one", keys(0)))
    val newBlock = block.copy(transactions = badTxs)
    newBlock(state).left.value mustBe IdentityNotClaimedError("two")
  }

  it should "calculate keys correctly" in {
    val keys = (1 to 2).map(_ => generateKeyPair._1)

    val txs = List (Claim("one", keys(0)),
      Link("two", keys(1)),
      Unlink("three", keys(1)),
      Unlink("three", keys(0)))
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    block.partitionIds mustBe Set("one", "two", "three")
  }
}
