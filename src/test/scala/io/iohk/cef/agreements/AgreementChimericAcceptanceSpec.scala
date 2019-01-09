package io.iohk.cef.agreements

import io.iohk.cef.agreements.AgreementFixture._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger.chimeric.ChimericTx
import org.scalatest.FlatSpec

class AgreementChimericAcceptanceSpec extends FlatSpec {
  /*
  Smart Agreements in the Chimeric Ledger:
  User A and User B want to exchange with each other 10 C1 coins for 20 C2 coins, and the exchange should happen atomically.
  We assume that, in the current ledger state, user A has an unspent output OutA of 10 C1 coins and user B has an unspent output OutB of 20 C2 coins.
  User A initiates the construction of the transaction: it creates a transaction that has the following transaction fragments:
  two inputs InA (spending OutA) and InB (spending OutB) and two outputs OutA' (of 20 C2 coins to A's address) and OutB'
  (of 10 C1 coins to B's address).
  Additionally, User A adds another transaction fragment signing the input InA.
  The transaction is now currently incomplete and invalid, because input InB requires a signature from user B.
  User A sends the incomplete transaction to user B.
  User B adds another transaction fragment signing the input InB, thereby completing the transaction.
  (NB: User B might not be party to the ledger. We assume that User A is party to the ledger because they initiated the interaction).
  Either:
  User B adds the completed transaction to the ledger directly.
  Or
  User B replies with an Agreement containing the completed transaction
  User A adds it to the ledger.
   */
  it should "support the creation of a ChimericTx" in forTwoArbitraryAgreementPeers[ChimericTx] { (alice, bob) =>

  }
}
