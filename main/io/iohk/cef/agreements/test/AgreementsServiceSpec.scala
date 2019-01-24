package io.iohk.cef.agreements

import java.util.UUID

import io.iohk.cef.agreements.AgreementFixture._
import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.test.DummyNoMessageConversationalNetwork
import monix.execution.schedulers.TestScheduler
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar.mock

import scala.concurrent.duration._

class AgreementsServiceSpec extends FlatSpec {

  implicit val patienceConfig = PatienceConfig(timeout = 1000 millis, interval = 200 millis)

  behavior of "AgreementsService"

  it should "support proposals" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID()
    val data = "it rained on Saturday"
    val bobsHandler: AgreementMessage[String] => Unit = mock[AgreementMessage[String] => Unit]

    // when
    bob.agreementsService.agreementEvents.foreach(event => bobsHandler(event))
    alice.agreementsService.propose(id, data, Set(bob.nodeId))

    // then
    eventually {
      verify(bobsHandler).apply(Propose(id, alice.nodeId, data))
    }
  }

  it should "support agreeing to a proposal" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID()
    val proposedData = "it rained on Saturday"
    val agreedData = "it rained on Saturday and Sunday"
    val aliceHandler = mock[AgreementMessage[String] => Unit]

    // when
    alice.agreementsService.agreementEvents.foreach(event => aliceHandler(event))
    bob.agreementsService.agreementEvents.foreach(event => {
      bob.agreementsService.agree(event.correlationId, agreedData)
    })
    alice.agreementsService.propose(id, proposedData, Set(bob.nodeId))

    // then
    eventually {
      verify(aliceHandler).apply(Agree(id, bob.nodeId, agreedData))
    }
  }

  it should "support declining a proposal" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID()
    val proposedData = "it rained on Saturday"
    val aliceHandler = mock[AgreementMessage[String] => Unit]

    // when
    alice.agreementsService.agreementEvents.foreach(event => aliceHandler(event))
    bob.agreementsService.agreementEvents.foreach(event => bob.agreementsService.decline(event.correlationId))
    alice.agreementsService.propose(id, proposedData, Set(bob.nodeId))

    // then
    eventually {
      verify(aliceHandler).apply(Decline(id, bob.nodeId))
    }
  }

  it should "throw an exception if the proposal''s recipient list is empty" in {
    implicit val scheduler = TestScheduler()
    val network = new DummyNoMessageConversationalNetwork[AgreementMessage[String]]()
    val service = new AgreementsServiceImpl[String](network)

    intercept[IllegalArgumentException] {
      service.propose(UUID.randomUUID(), "data", Set())
    }
  }

  it should "throw when agreeing to a non-existent proposal" in forTwoArbitraryAgreementPeers[String] { (alice, _) =>
    val id = UUID.randomUUID()

    // when
    val exception = the[IllegalArgumentException] thrownBy alice.agreementsService.agree(id, "anything")

    // then
    exception.getMessage shouldBe "requirement failed: Unknown correlationId '${id}'."
  }

  it should "throw when declining a non-existent proposal" in forTwoArbitraryAgreementPeers[String] { (alice, _) =>
    val id = UUID.randomUUID()

    // when
    val exception = the[IllegalArgumentException] thrownBy alice.agreementsService.decline(id)

    // then
    exception.getMessage shouldBe s"Unknown correlationId '${id}'."
  }
}
