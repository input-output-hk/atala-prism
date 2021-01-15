package io.iohk.atala.mirror.db

import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import io.iohk.atala.prism.models.ConnectorMessageId

import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *db.UserCredentialDaoSpec"
class UserCredentialDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._, UserCredentialFixtures._

  "UserCredentialDao" should {
    "insert single user credential into the db" in {
      // when
      val resultCount = (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- UserCredentialDao.insert(userCredential1)
      } yield resultCount)
        .transact(database)
        .runSyncUnsafe()

      // then
      resultCount mustBe 1
    }

    "return user credentials by user id" in {
      // given
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        _ <- UserCredentialDao.insert(userCredential1)
        _ <- UserCredentialDao.insert(userCredential3)
      } yield ()).transact(database).runSyncUnsafe()

      // when
      val userCredentials = UserCredentialDao.findBy(userCredential1.connectionToken).transact(database).runSyncUnsafe()

      // then
      userCredentials mustBe List(userCredential1)
    }

    "return none if a user credentials don't exist" in {
      // when
      val userCredentials = UserCredentialDao.findBy(ConnectionToken("non existing")).transact(database).runSyncUnsafe()

      // then
      userCredentials.size mustBe 0
    }

    "return last seen message id" in {
      // given
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        _ <- UserCredentialDao.insert(userCredential1)
        _ <- UserCredentialDao.insert(userCredential3)
      } yield ()).transact(database).runSyncUnsafe()

      // when
      val lastSeenMessageId: Option[ConnectorMessageId] =
        UserCredentialDao.findLastSeenMessageId.transact(database).runSyncUnsafe()

      // then
      lastSeenMessageId mustBe Some(userCredential3.messageId)
    }
  }
}
