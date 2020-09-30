package io.iohk.atala.mirror.db

import java.util.UUID

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.models.{Connection, UserCredentials}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredentials.{IssuersDID, RawCredential}

class UserCredentialsDaoSpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "UserCredentialsDao" should {
    "insert single user credentials into the db" in new UserCredentialsFixtures {
      (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- UserCredentialsDao.insert(userCredentials1)
      } yield resultCount)
        .transact(database)
        .unsafeRunSync() mustBe 1
    }

    "return user credentials by user id" in new UserCredentialsFixtures {
      val insertAll: ConnectionIO[Unit] = for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        _ <- UserCredentialsDao.insert(userCredentials1)
        _ <- UserCredentialsDao.insert(userCredentials2)
      } yield ()

      (for {
        _ <- insertAll
        userCredentials <- UserCredentialsDao.findBy(userCredentials1.connectionToken)
      } yield userCredentials).transact(database).unsafeRunSync() mustBe Some(userCredentials1)
    }

    "return none if a user credentials don't exist" in new UserCredentialsFixtures {
      UserCredentialsDao.findBy(ConnectionToken("non exisiting")).transact(database).unsafeRunSync() mustBe None
    }
  }

  trait UserCredentialsFixtures {
    val uuid = UUID.randomUUID()
    val connection1 = Connection(ConnectionToken("token1"), None, ConnectionState.Invited)
    val connection2 = Connection(ConnectionToken("token2"), Some(ConnectionId(uuid)), ConnectionState.Invited)
    val userCredentials1 =
      UserCredentials(connection1.token, RawCredential("rawCredential1"), IssuersDID("issuersDID1"))
    val userCredentials2 =
      UserCredentials(connection2.token, RawCredential("rawCredential2"), IssuersDID("issuersDID2"))
  }
}
