package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.stubs.TrisaIntegrationServiceStub
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._
import monix.execution.Scheduler.Implicits.global

class TrisaServiceSpec extends AnyWordSpec with Matchers with MirrorFixtures {
  import ConnectorMessageFixtures._

  "TrisaService" should {
    "initiate transaction in trisa integration service" in new TrisaServiceFixtures {
      messageProcessor(initiateTrisaCardanoTransactionMessage).value.runSyncUnsafe() mustBe Right(None)
    }

    "return None if ReceivedMessage is not CardanoAddressMessage" in new TrisaServiceFixtures {
      messageProcessor(credentialMessage1) mustBe None
    }
  }

  trait TrisaServiceFixtures {
    val stub = new TrisaIntegrationServiceStub()
    val trisaService = new TrisaService(stub)
    val messageProcessor = trisaService.initiateTrisaCardanoTransactionMessageProcessor
  }

}
