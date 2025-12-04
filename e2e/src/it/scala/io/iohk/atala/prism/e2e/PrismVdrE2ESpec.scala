package io.iohk.atala.prism.e2e

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// Scaffold for VDR gRPC end-to-end coverage; to be implemented with full flows.
class PrismVdrE2ESpec extends AnyWordSpec with Matchers {

  "VDR gRPC flow" should {
    "create, update, and delete a VDR resource" taggedAs (E2ETestTag) ignore {
      pending
    }
  }
}
