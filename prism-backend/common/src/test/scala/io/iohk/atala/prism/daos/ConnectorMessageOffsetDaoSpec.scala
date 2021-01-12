package io.iohk.atala.prism.daos

import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import doobie.implicits._

// sbt "project common" "testOnly *daos.ConnectorMessageOffsetDaoSpec"
class ConnectorMessageOffsetDaoSpec extends PostgresRepositorySpec {

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
