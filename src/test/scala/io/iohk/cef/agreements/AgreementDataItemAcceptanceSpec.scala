package io.iohk.cef.agreements

import io.iohk.cef.agreements.AgreementFixture._
import io.iohk.cef.agreements.AgreementsMessage.Propose
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.utils.NonEmptyList
import io.iohk.cef.data.{DataItem, Owner, Witness}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, whenReady}
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Future

class AgreementDataItemAcceptanceSpec extends FlatSpec {

  behavior of "AgreementsService"

  implicit val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Seconds))

  /*
  Smart Agreements in the Data Item Framework:

  Assume a data item type that contains an arbitrary statement and signatures from users that agree with that statement.
    User A creates a data item with the statement it rained on 01/12/2018 in Wollongong and wants A, B and C as witnesses of this statement.
    User A adds itself as a witness.

  The data item is currently incomplete, because signatures from B and C are missing.

  It sends the incomplete data item to the remaining witnesses.
    User B receives the data item, signs the statement and adds that signature to the data item (creating a copy of the data item)
    User B sends an Agreement to User A containing the data item with its signature. C does the same as B.

  A collates the the signatures into a version of the original data item that contains all the required signatures.

  A can now file the data item.
   */
  it should "support the creation of DataItems" in forThreeArbitraryAgreementPeers[DataItem[String]] {
    (alice, bob, charlie) =>
      // given
      val data = "it rained on 01/12/2018 in Wollongong"
      val dataItem = aWitnessedDataItem(data, alice.keyPair)
      willWitnessAndAgree(bob)
      willWitnessAndAgree(charlie)

      // when
      val collation: Future[DataItem[String]] =
        alice.agreementsService.agreementEvents.take(2).fold(dataItem)(collateSignatures)
      alice.agreementsService.propose("correlation-id", dataItem, Set(bob.nodeId, charlie.nodeId))

      // then
      whenReady(collation) { agreedDataItem =>
        agreedDataItem.witnesses should contain(witness(data, alice.keyPair))
        agreedDataItem.witnesses should contain(witness(data, bob.keyPair))
        agreedDataItem.witnesses should contain(witness(data, charlie.keyPair))
      }
  }

  private def collateSignatures(acc: DataItem[String], next: AgreementMessage[DataItem[String]]): DataItem[String] = {
    AgreementsMessage.catamorphism[DataItem[String], DataItem[String]](
      fPropose = _ => acc,
      fAgree = agree => acc.copy(witnesses = agree.data.witnesses.head :: acc.witnesses.toList),
      fDecline = _ => acc
    )(next)
  }

  private def willWitnessAndAgree(agreementFixture: AgreementFixture[DataItem[String]]): Unit = {
    def agreeToProposal(proposal: Propose[DataItem[String]]): Unit = {
      agreementFixture.agreementsService
        .agree(proposal.correlationId, witnessDataItem(proposal.data, agreementFixture.keyPair))
    }
    agreementFixture.agreementsService.agreementEvents
      .foreach(
        message => AgreementsMessage.catamorphism[DataItem[String], Unit](agreeToProposal, _ => (), _ => ())(message)
      )
  }

  private def aWitnessedDataItem(data: String, keyPair: SigningKeyPair): DataItem[String] = {
    val itemId = "item-id"
    val signature = sign(data, keyPair.`private`)
    val aw = Witness(keyPair.public, signature)
    val owner = Owner(keyPair.public, signature)

    new DataItem[String](itemId, data, Seq(aw), NonEmptyList(owner))
  }

  private def witnessDataItem(dataItem: DataItem[String], keyPair: SigningKeyPair): DataItem[String] =
    dataItem.copy(witnesses = witness(dataItem.data, keyPair) :: dataItem.witnesses.toList)

  private def witness(data: String, keyPair: SigningKeyPair): Witness = {
    val signature = sign(data, keyPair.`private`)
    Witness(keyPair.public, signature)
  }
}
