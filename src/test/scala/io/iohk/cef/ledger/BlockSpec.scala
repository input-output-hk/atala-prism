package io.iohk.cef.ledger

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.crypto.low.DigitalSignature
import io.iohk.cef.ledger.identity._
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

class BlockSpec
    extends FlatSpec
    with MustMatchers
    with RSAKeyGenerator
    with EitherValues {

  behavior of "Block"

  val dummySignature = new DigitalSignature(ByteString.empty)

  it should "apply itself to a state" in {
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val txs = List (
      Claim("one", pair1._1, IdentityTransaction.sign("one", pair1._1, pair1._2)),
      Link("one", pair2._1, IdentityTransaction.sign("one", pair2._1, pair1._2)),
      Unlink("one", pair2._1, IdentityTransaction.sign("one", pair2._1, pair1._2)),
      Unlink("one", pair1._1, IdentityTransaction.sign("one", pair1._1, pair1._2)))

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    val state = new IdentityLedgerState(Map())
    block(state).right.value mustBe IdentityLedgerState(Map())

    val badTxs = List (
      Claim("one", pair1._1, IdentityTransaction.sign("one", pair1._1, pair1._2)),
      Link("two", pair2._1, IdentityTransaction.sign("two", pair2._1, pair1._2)),
      Unlink("one", pair2._1, dummySignature),
      Unlink("one", pair1._1, dummySignature))
    val newBlock = block.copy(transactions = badTxs)
    newBlock(state).left.value mustBe IdentityNotClaimedError("two")
  }

  it should "calculate keys correctly" in {
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val txs = List (
      Claim("one", pair1._1, dummySignature),
      Link("two", pair2._1, IdentityTransaction.sign("two", pair2._1, pair1._2)),
      Unlink("three", pair2._1, dummySignature),
      Unlink("three", pair1._1, dummySignature))

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val block = Block(header, txs)
    block.partitionIds mustBe Set("one", "two", "three")
  }
}
