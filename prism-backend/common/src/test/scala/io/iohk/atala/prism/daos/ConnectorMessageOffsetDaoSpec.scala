package io.iohk.atala.prism.daos

import io.iohk.atala.prism.models.ConnectorMessageId
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec

// sbt "project common" "testOnly *daos.ConnectorMessageOffsetDaoSpec"
class ConnectorMessageOffsetDaoSpec extends AtalaWithPostgresSpec {

  override protected def migrationScriptsLocation: String = "common/db/migration"

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
