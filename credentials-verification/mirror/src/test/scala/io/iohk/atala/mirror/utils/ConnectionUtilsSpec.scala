package io.iohk.atala.mirror.utils

import java.util.UUID

import io.iohk.atala.mirror.models.Connection.ConnectionId
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

// sbt "project mirror" "testOnly *utils.ConnectionUtilsSpec"
class ConnectionUtilsSpec extends AnyWordSpec with Matchers {

  "parseConnectionId" should {
    "parse connection id" in {
      val uuid = "fa50dc39-df71-47c7-b43f-8113871a4e53"

      ConnectionUtils.parseConnectionId("") mustBe None
      ConnectionUtils.parseConnectionId("wrong") mustBe None
      ConnectionUtils.parseConnectionId(uuid) mustBe Some(
        ConnectionId(UUID.fromString(uuid))
      )
    }
  }

}
