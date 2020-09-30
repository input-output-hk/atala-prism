package io.iohk.atala.mirror.db

import java.util.UUID

import scala.concurrent.duration._

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken, ConnectionState}

import doobie.implicits._

class ConnectionDaoSpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "ConnectionDao" should {
    "insert single connection into the db" in new ConnectionFixtures {
      ConnectionDao
        .insert(connection1)
        .transact(database)
        .unsafeRunSync() mustBe 1
    }

    "return connection by the token" in new ConnectionFixtures {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findBy(connection1.token)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection1)
    }

    "return connection by the id" in new ConnectionFixtures {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findBy(ConnectionId(uuid))
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection2)
    }

    "return none if a token doesn't exist" in new ConnectionFixtures {
      ConnectionDao.findBy(ConnectionToken("token")).transact(database).unsafeRunSync() mustBe None
      ConnectionDao.findBy(ConnectionId(UUID.randomUUID())).transact(database).unsafeRunSync() mustBe None
    }
  }

  trait ConnectionFixtures {
    val uuid = UUID.randomUUID()
    val connection1 = Connection(ConnectionToken("token1"), None, ConnectionState.Invited)
    val connection2 =
      Connection(ConnectionToken("token2"), Some(ConnectionId(uuid)), ConnectionState.Invited)
  }
}
