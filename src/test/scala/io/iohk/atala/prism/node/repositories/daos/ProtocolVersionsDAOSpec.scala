package io.iohk.atala.prism.node.repositories.daos

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.models.ProtocolVersion.ProtocolVersion1_0

class ProtocolVersionsDAOSpec extends AtalaWithPostgresSpec {
  "ProtocolVersionsDAO" should {
    "return initial protocol version" in {
      ProtocolVersionsDAO.getCurrentProtocolVersion
        .transact(database)
        .unsafeRunSync() mustBe ProtocolVersion1_0
    }
  }
}
