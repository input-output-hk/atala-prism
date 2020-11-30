package io.iohk.atala.mirror.db

import java.util.UUID

import cats.data.NonEmptyList

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import io.iohk.atala.prism.identity.DID

// sbt "project mirror" "testOnly *db.ConnectionDaoSpec"
class ConnectionDaoSpec extends PostgresRepositorySpec with MirrorFixtures {
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
        connection <- ConnectionDao.findByConnectionToken(connection1.token)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connectionWithNewId)
    }

    "return connection by the token" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByConnectionToken(connection1.token)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection1)
    }

    "return connection by the id" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByConnectionId(connectionId2)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection2)
    }

    "return connection by the holder did" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByHolderDID(connectionHolderDid2)
      } yield connection).transact(database).unsafeRunSync() mustBe Some(connection2)
    }

    "return connection by pay id name" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByPayIdName(connectionPayIdName2)
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
      ConnectionDao.findByConnectionToken(ConnectionToken("token")).transact(database).unsafeRunSync() mustBe None
    }

    "return none if a connection id doesn't exist" in {
      ConnectionDao.findByConnectionId(ConnectionId(UUID.randomUUID())).transact(database).unsafeRunSync() mustBe None
    }

    "return none if a holder DID doesn't exist" in {
      ConnectionDao.findByHolderDID(DID.buildPrismDID("none")).transact(database).unsafeRunSync() mustBe None
    }

    "return last seen connection id" in {
      // given
      ConnectionFixtures.insertAll(database).unsafeRunSync()

      // when
      val lastSeenConnectionId: Option[ConnectionId] =
        ConnectionDao.findLastSeenConnectionId.transact(database).unsafeRunSync()

      // then
      lastSeenConnectionId mustBe connection2.id
    }
  }
}
