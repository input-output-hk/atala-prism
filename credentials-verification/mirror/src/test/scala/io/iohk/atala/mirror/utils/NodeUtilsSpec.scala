package io.iohk.atala.mirror.utils

import io.iohk.atala.mirror.stubs.NodeClientServiceStub
import io.iohk.atala.mirror.{MirrorFixtures, NodeUtils}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *utils.NodeUtilsSpec"
class NodeUtilsSpec extends AnyWordSpec with Matchers with MirrorFixtures {
  import CredentialFixtures._

  "getKeyData" should {
    "return key data" in {
      NodeUtils.getKeyData(issuerDID, issuanceKeyId, defaultNodeClientStub).value.runSyncUnsafe() mustBe a[Right[_, _]]
    }

    "return error when key data is not available" in {
      val nodeClientStub = new NodeClientServiceStub
      NodeUtils.getKeyData(issuerDID, issuanceKeyId, nodeClientStub).value.runSyncUnsafe() mustBe a[Left[_, _]]
    }
  }

}
