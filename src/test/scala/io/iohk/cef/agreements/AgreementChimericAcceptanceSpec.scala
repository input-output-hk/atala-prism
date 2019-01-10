package io.iohk.cef.agreements

import io.iohk.cef.agreements.AgreementFixture._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.chimeric.SignatureTxFragment.signFragments
import io.iohk.cef.ledger.chimeric.{SignatureTxFragment, _}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, whenReady}
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Future

class AgreementChimericAcceptanceSpec extends FlatSpec {

  behavior of "AgreementsService"

  implicit val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Seconds))

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

  User B replies with an Agreement containing the completed transaction
  User A can now add it to the ledger.
   */
  it should "support the creation of a ChimericTx" in forTwoArbitraryAgreementPeers[ChimericTx] { (alice, bob) =>
    // given
    val tx = ChimericTx(
      signFragments(
        List(
          Input(TxOutRef("OutA", 0), Value("C1" -> BigDecimal("10"))),
          Input(TxOutRef("OutB", 0), Value("C2" -> BigDecimal("20"))),
          Output(Value("C1" -> BigDecimal("10")), bob.keyPair.public),
          Output(Value("C2" -> BigDecimal("20")), alice.keyPair.public)
        ),
        alice.keyPair.`private`
      )
    )

    willSign(bob)

    // when
    val bobsAgreement: Future[Agree[ChimericTx]] =
      alice.agreementsService.agreementEvents.map(_.asInstanceOf[Agree[ChimericTx]]).head()
    alice.agreementsService.propose("correlation-id", tx, List(bob.nodeId))

    whenReady(bobsAgreement) { agreement =>
      val signatureFragments = agreement.data.fragments.collect { case s: SignatureTxFragment => s }

      signatureFragments(0) shouldBe SignatureTxFragment(tx.fragments, alice.keyPair.`private`)
      signatureFragments(1) shouldBe SignatureTxFragment(tx.fragments, bob.keyPair.`private`)
    }
  }

  private def willSign(agreementFixture: AgreementFixture[ChimericTx]): Unit = {
    def agreeToProposal(proposal: Propose[ChimericTx]): Unit = {
      agreementFixture.agreementsService.agree(proposal.correlationId, signTx(proposal.data, agreementFixture.keyPair))
    }
    agreementFixture.agreementsService.agreementEvents
      .foreach(message => AgreementsMessage.catamorphism[ChimericTx, Unit](agreeToProposal, _ => (), _ => ())(message))
  }

  private def signTx(tx: ChimericTx, keyPair: SigningKeyPair): ChimericTx =
    ChimericTx(signFragments(tx.fragments, keyPair.`private`))

}
