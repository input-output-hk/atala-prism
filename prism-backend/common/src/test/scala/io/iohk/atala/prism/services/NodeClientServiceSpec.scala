package io.iohk.atala.prism.services

import io.iohk.atala.prism.stubs.NodeClientServiceStub
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.services.NodeClientService

class NodeClientServiceSpec extends AnyWordSpec with Matchers with ServicesFixtures {
  import CredentialFixtures._

  "getKeyData" should {
    "return key data" in {
      NodeClientService
        .getKeyData(issuerDID, issuanceKeyId, defaultNodeClientStub)
        .value
        .runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when key data is not available" in {
      val nodeClientStub = new NodeClientServiceStub
      NodeClientService.getKeyData(issuerDID, issuanceKeyId, nodeClientStub).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

}
