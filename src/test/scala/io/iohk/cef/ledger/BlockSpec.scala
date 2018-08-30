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
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val txs = List (
      Claim("one", pair1._1),
      Link("one", pair2._1, Link.sign("one", pair2._1, pair1._2)),
      Unlink("one", pair2._1),
      Unlink("one", pair1._1))

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    val state = new IdentityLedgerState(Map())
    val newState = block(state)

    newState.right.value mustBe IdentityLedgerState(Map())

    val badTxs = List (
      Claim("one", pair1._1),
      Link("two", pair2._1, Link.sign("two", pair2._1, pair1._2)),
      Unlink("one", pair2._1),
      Unlink("one", pair1._1))
    val newBlock = block.copy(transactions = badTxs)
    newBlock(state).left.value mustBe IdentityNotClaimedError("two")
  }

  it should "calculate keys correctly" in {
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val txs = List (
      Claim("one", pair1._1),
      Link("two", pair2._1, Link.sign("two", pair2._1, pair1._2)),
      Unlink("three", pair2._1),
      Unlink("three", pair1._1))

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    block.partitionIds mustBe Set("one", "two", "three")
  }
}
