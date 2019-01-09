package io.iohk.cef.agreements

import java.util.UUID

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.{ConversationalNetwork, NodeId}
import io.iohk.cef.network.transport.tcp.NetUtils
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar.mock

import scala.concurrent.duration._
import scala.reflect.runtime.universe._

class AgreementsServiceSpec extends FlatSpec {

  implicit val patienceConfig = PatienceConfig(timeout = 1000 millis, interval = 200 millis)

  behavior of "AgreementsService"

  it should "support proposals" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID().toString
    val data = "it rained on Saturday"
    val bobsHandler: AgreementMessage[String] => Unit = mock[AgreementMessage[String] => Unit]

    // when
    bob.agreementsService.agreementEvents.foreach(event => bobsHandler(event))
    alice.agreementsService.propose(id, data, List(bob.nodeId))

    // then
    eventually {
      verify(bobsHandler).apply(Propose(id, alice.nodeId, data))
    }
  }

  it should "support agreeing to a proposal" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID().toString
    val proposedData = "it rained on Saturday"
    val agreedData = "it rained on Saturday and Sunday"
    val aliceHandler = mock[AgreementMessage[String] => Unit]

    // when
    alice.agreementsService.agreementEvents.foreach(event => aliceHandler(event))
    bob.agreementsService.agreementEvents.foreach(event => {
      bob.agreementsService.agree(event.correlationId, agreedData)
    })
    alice.agreementsService.propose(id, proposedData, List(bob.nodeId))

    // then
    eventually {
      verify(aliceHandler).apply(Agree(id, bob.nodeId, agreedData))
    }
  }

  it should "support declining a proposal" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID().toString
    val proposedData = "it rained on Saturday"
    val aliceHandler = mock[AgreementMessage[String] => Unit]

    // when
    alice.agreementsService.agreementEvents.foreach(event => aliceHandler(event))
    bob.agreementsService.agreementEvents.foreach(event => bob.agreementsService.decline(event.correlationId))
    alice.agreementsService.propose(id, proposedData, List(bob.nodeId))

    // then
    eventually {
      verify(aliceHandler).apply(Decline(id, bob.nodeId))
    }
  }

  it should "throw when agreeing to a non-existent proposal" in forTwoArbitraryAgreementPeers[String] { (alice, _) =>
    // when
    val exception = the [IllegalArgumentException] thrownBy alice.agreementsService.agree("foo", "anything")

    // then
    exception.getMessage shouldBe "Unknown correlationId 'foo'."
  }

  it should "throw when declining a non-existent proposal" in forTwoArbitraryAgreementPeers[String] { (alice, _) =>
    // when
    val exception = the [IllegalArgumentException] thrownBy alice.agreementsService.decline("foo")

    // then
    exception.getMessage shouldBe "Unknown correlationId 'foo'."
  }


  case class AgreementFixture[T](nodeId: NodeId, agreementsService: AgreementsService[T])

  def forTwoArbitraryAgreementPeers[T: NioCodec : TypeTag](testCode: (AgreementFixture[T], AgreementFixture[T]) => Any): Unit = {
    NetUtils.forTwoArbitraryNetworkPeers { (aliceFix, bobFix) =>
      val aliceNet = new ConversationalNetwork[AgreementMessage[T]](aliceFix.networkDiscovery, aliceFix.transports)
      val aliceAgreementService = new AgreementsService[T](aliceNet)

      val bobNet = new ConversationalNetwork[AgreementMessage[T]](bobFix.networkDiscovery, bobFix.transports)
      val bobAgreementService = new AgreementsService[T](bobNet)

      testCode(AgreementFixture(aliceFix.nodeId, aliceAgreementService), AgreementFixture(bobFix.nodeId, bobAgreementService))
    }
  }
}
