package io.iohk.atala.mirror.db

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.ConnectorMessageId
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import doobie.implicits._

// sbt "project mirror" "testOnly *db.ConnectorMessageOffsetDaoSpec"
class ConnectorMessageOffsetDaoSpec extends PostgresRepositorySpec with MirrorFixtures {

  "ConnectorMessageOffsetDao" should {
    "update and return last seen message id" in {
      // when
      val messageIdOption = (for {
        _ <- ConnectorMessageOffsetDao.updateLastMessageOffset(ConnectorMessageId("id1"))
        _ <- ConnectorMessageOffsetDao.updateLastMessageOffset(ConnectorMessageId("id1"))
        _ <- ConnectorMessageOffsetDao.updateLastMessageOffset(ConnectorMessageId("id2"))
        messageIdOption <- ConnectorMessageOffsetDao.findLastMessageOffset()
      } yield messageIdOption)
        .transact(database)
        .unsafeRunSync()

      // then
      messageIdOption mustBe Some(ConnectorMessageId("id2"))
    }
  }
}
