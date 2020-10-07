package io.iohk.atala.mirror.db

import java.util.UUID

import cats.data.NonEmptyList

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import doobie.implicits._
import io.iohk.atala.mirror.fixtures.ConnectionFixtures

// mill -i mirror.test.single io.iohk.atala.mirror.db.ConnectionDaoSpec
class ConnectionDaoSpec extends PostgresRepositorySpec {
  import ConnectionFixtures._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "ConnectionDao" should {
    "insert single connection into the db" in {
      ConnectionDao
        .insert(connection1)
        .transact(database)
        .unsafeRunSync() mustBe 1
    }

    "update conection row" in {
      val connectionWithNewId = connection1.copy(id = connection2.id)
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.update(connectionWithNewId)
        connection <- ConnectionDao.findBy(connection1.token)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connectionWithNewId)
    }

    "return connection by the token" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findBy(connection1.token)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection1)
    }

    "return connection by the id" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findBy(connectionId2)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection2)
    }

    "return connection by many ids" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connections <- ConnectionDao.findBy(NonEmptyList.of(connectionId2))
      } yield connections).transact(database).unsafeRunSync() mustBe List(connection2)
    }

    "return none if a token doesn't exist" in {
      ConnectionDao.findBy(ConnectionToken("token")).transact(database).unsafeRunSync() mustBe None
      ConnectionDao.findBy(ConnectionId(UUID.randomUUID())).transact(database).unsafeRunSync() mustBe None
    }

    "return last seen connection id" in {
      // given
      insertAllConnections(database).unsafeRunSync()

      // when
      val lastSeenConnectionId: Option[ConnectionId] =
        ConnectionDao.findLastSeenConnectionId.transact(database).unsafeRunSync()

      // then
      lastSeenConnectionId mustBe connection2.id
    }
  }
}
