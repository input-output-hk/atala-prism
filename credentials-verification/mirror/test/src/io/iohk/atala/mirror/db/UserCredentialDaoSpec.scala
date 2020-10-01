package io.iohk.atala.mirror.db

import java.util.UUID

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.models.{Connection, UserCredential}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredential.{IssuersDID, RawCredential}

class UserCredentialDaoSpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "UserCredentialDao" should {
    "insert single user credential into the db" in new UserCredentialFixtures {
      (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- UserCredentialDao.insert(userCredential1)
      } yield resultCount)
        .transact(database)
        .unsafeRunSync() mustBe 1
    }

    "return user credential by user id" in new UserCredentialFixtures {
      val insertAll: ConnectionIO[Unit] = for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        _ <- UserCredentialDao.insert(userCredential1)
        _ <- UserCredentialDao.insert(userCredential2)
      } yield ()

      (for {
        _ <- insertAll
        userCredential <- UserCredentialDao.findBy(userCredential1.connectionToken)
      } yield userCredential).transact(database).unsafeRunSync() mustBe Some(userCredential1)
    }

    "return none if a user credential doesn't exist" in new UserCredentialFixtures {
      UserCredentialDao.findBy(ConnectionToken("non existing")).transact(database).unsafeRunSync() mustBe None
    }
  }

  trait UserCredentialFixtures {
    val uuid = UUID.randomUUID()
    val connection1 = Connection(ConnectionToken("token1"), None, ConnectionState.Invited)
    val connection2 = Connection(ConnectionToken("token2"), Some(ConnectionId(uuid)), ConnectionState.Invited)
    val userCredential1 =
      UserCredential(connection1.token, RawCredential("rawCredential1"), Some(IssuersDID("issuersDID1")))
    val userCredential2 =
      UserCredential(connection2.token, RawCredential("rawCredential2"), None)
  }
}
