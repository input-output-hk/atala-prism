package io.iohk.atala.mirror.db

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import io.iohk.atala.prism.models.ConnectorMessageId

// sbt "project mirror" "testOnly *db.UserCredentialDaoSpec"
class UserCredentialDaoSpec extends PostgresRepositorySpec with MirrorFixtures {
  import ConnectionFixtures._, UserCredentialFixtures._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "UserCredentialDao" should {
    "insert single user credential into the db" in {
      // when
      val resultCount = (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- UserCredentialDao.insert(userCredential1)
      } yield resultCount)
        .transact(database)
        .unsafeRunSync()

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
      } yield ()).transact(database).unsafeRunSync()

      // when
      val userCredentials = UserCredentialDao.findBy(userCredential1.connectionToken).transact(database).unsafeRunSync()

      // then
      userCredentials mustBe List(userCredential1)
    }

    "return none if a user credentials don't exist" in {
      // when
      val userCredentials = UserCredentialDao.findBy(ConnectionToken("non existing")).transact(database).unsafeRunSync()

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
      } yield ()).transact(database).unsafeRunSync()

      // when
      val lastSeenMessageId: Option[ConnectorMessageId] =
        UserCredentialDao.findLastSeenMessageId.transact(database).unsafeRunSync()

      // then
      lastSeenMessageId mustBe Some(userCredential3.messageId)
    }
  }
}
