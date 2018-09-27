package io.iohk.cef.ledger

import java.time.Instant

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.ledger.identity._
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

class BlockSpec extends FlatSpec with MustMatchers with EitherValues with SigningKeyPairs {

  behavior of "Block"

  it should "apply itself to a state" in {
    val txs = List(
      Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`)),
      Link("one", bob.public, IdentityTransaction.sign("one", bob.public, alice.`private`)),
      Unlink("one", bob.public, IdentityTransaction.sign("one", bob.public, alice.`private`)),
      Unlink("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`))
    )

    val header = IdentityBlockHeader(Instant.now)
    val block = Block(header, txs)
    val state = new IdentityLedgerState(Map())
    block(state).right.value mustBe IdentityLedgerState(Map())

    val badTxs = List(
      Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`)),
      Link("two", bob.public, IdentityTransaction.sign("two", bob.public, alice.`private`)),
      Unlink("one", bob.public, uselessSignature),
      Unlink("one", alice.public, uselessSignature)
    )
    val newBlock = block.copy(transactions = badTxs)
    newBlock(state).left.value mustBe IdentityNotClaimedError("two")
  }

  it should "calculate keys correctly" in {
    val txs = List(
      Claim("one", alice.public, uselessSignature),
      Link("two", bob.public, IdentityTransaction.sign("two", bob.public, alice.`private`)),
      Unlink("three", bob.public, uselessSignature),
      Unlink("three", alice.public, uselessSignature)
    )

    val header = IdentityBlockHeader(Instant.now)
    val block = Block(header, txs)
    block.partitionIds mustBe Set("one", "two", "three")
  }
}
