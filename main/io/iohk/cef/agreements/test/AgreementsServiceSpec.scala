package io.iohk.cef.agreements

import java.util.UUID

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.NodeId
import io.iohk.cef.network.transport.tcp.NetUtils
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar.mock
import org.mockito.Mockito.{timeout, verify}
//import org.scalatest.concurrent.Eventually._
import org.scalatest.Matchers._
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures._
import scala.reflect.runtime.universe._

class AgreementsServiceSpec extends FlatSpec {

  val verifyDelayMillis = 500
  implicit val patienceConfig = PatienceConfig(timeout = 5000 millis, interval = 1000 millis)

  behavior of "AgreementsService"

  ignore should "support proposals" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID().toString
    val data = "it rained on Saturday"
    val bobsMockHandler: AgreementMessage[String] => Unit = mock[AgreementMessage[String] => Unit]
    val bobsHandler: AgreementMessage[String] => Unit = msg => {
      println(s"Bob got message: $msg")
      bobsMockHandler.apply(msg)
    }

    // when
    bob.agreementsService.agreementEvents.foreach(event => bobsHandler(event))
    alice.agreementsService.propose(id, data, List(bob.nodeId))

    // then
    verify(bobsMockHandler, timeout(verifyDelayMillis)).apply(Propose(id, alice.nodeId, data))
  }

  it should "support agreeing to a proposal" in forTwoArbitraryAgreementPeers[String] { (alice, bob) =>
    // given
    val id = UUID.randomUUID().toString
    val proposedData = "it rained on Saturday"
    val agreedData = "it rained on Saturday and Sunday"
    var aliceGets: List[AgreementMessage[String]] = List()
    println(s"Alice is '${alice.nodeId}'")
    println(s"Bob is '${bob.nodeId}'")

    // when
    alice.agreementsService.agreementEvents.foreach(event => aliceGets = event :: aliceGets)
    bob.agreementsService.agreementEvents.foreach(event => {
      println(s"Bob gets $event and agrees")
      agree(bob.agreementsService, event, agreedData)
    })
    Thread.sleep(1000)
    alice.agreementsService.propose(id, proposedData, List(bob.nodeId))

    // then
    Thread.sleep(1000)
    aliceGets shouldBe List(Agree(id, bob.nodeId, agreedData))
  }

  private def agree(svc: AgreementsService[String], event: AgreementMessage[String], data: String): Unit = {
    println(s"In the test agree fn. got event $event with data $data")
    event match {
      case p: Propose[String] => {
        println(s"agreeing to $p")
        svc.agree(p.correlationId, data)
      }
      case _ => fail("Unexpected agreement event received.")
    }
  }

  case class AgreementFixture[T](nodeId: NodeId, agreementsService: AgreementsService[T])

  def forTwoArbitraryAgreementPeers[T: NioCodec : TypeTag](testCode: (AgreementFixture[T], AgreementFixture[T]) => Any): Unit = {
    NetUtils.forTwoArbitraryNetworkPeers { (aliceNet, bobNet) =>
      val aliceAgreementService = new AgreementsService[T](aliceNet.networkDiscovery, aliceNet.transports)
      val bobAgreementService = new AgreementsService[T](bobNet.networkDiscovery, bobNet.transports)
      testCode(AgreementFixture(aliceNet.nodeId, aliceAgreementService), AgreementFixture(bobNet.nodeId, bobAgreementService))
    }
  }
}
