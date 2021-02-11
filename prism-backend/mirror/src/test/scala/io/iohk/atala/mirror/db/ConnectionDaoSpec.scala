package io.iohk.atala.mirror.db

import monix.eval.Task
import cats.data.NonEmptyList
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *db.ConnectionDaoSpec"
class ConnectionDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._

  "ConnectionDao" should {
    "insert single connection into the db" in {
      ConnectionDao
        .insert(connection1)
        .transact(database)
        .runSyncUnsafe() mustBe 1
    }

    "update conection row" in {
      val connectionWithNewId = connection1.copy(id = connection2.id)
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.update(connectionWithNewId)
        connection <- ConnectionDao.findByConnectionToken(connection1.token)
      } yield connection)
        .transact(database)
        .runSyncUnsafe()
        //ignore updated at
        .map(_.copy(updatedAt = connectionWithNewId.updatedAt)) mustBe Some(connectionWithNewId)
    }

    "return connection by the token" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByConnectionToken(connection1.token)
      } yield connection).transact(database).runSyncUnsafe() mustBe Some(connection1)
    }

    "return connection by the id" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByConnectionId(connectionId2)
      } yield connection).transact(database).runSyncUnsafe() mustBe Some(connection2)
    }

    "return connection by the holder did" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByHolderDID(connectionHolderDid2)
      } yield connection).transact(database).runSyncUnsafe() mustBe Some(connection2)
    }

    "return connection by pay id name" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connection <- ConnectionDao.findByPayIdName(connectionPayIdName2)
      } yield connection).transact(database).runSyncUnsafe() mustBe Some(connection2)
    }

    "return connection by many ids" in {
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.insert(connection2)
        connections <- ConnectionDao.findBy(NonEmptyList.of(connectionId2))
      } yield connections).transact(database).runSyncUnsafe() mustBe List(connection2)
    }

    "return none if a token doesn't exist" in {
      ConnectionDao.findByConnectionToken(ConnectionToken("token")).transact(database).runSyncUnsafe() mustBe None
    }

    "return none if a connection id doesn't exist" in {
      ConnectionDao.findByConnectionId(ConnectionId.random()).transact(database).runSyncUnsafe() mustBe None
    }

    "return none if a holder DID doesn't exist" in {
      ConnectionDao.findByHolderDID(newDID()).transact(database).runSyncUnsafe() mustBe None
    }

    "return last seen connection id" in {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val lastSeenConnectionId: Option[ConnectionId] =
        ConnectionDao.findLastSeenConnectionId.transact(database).runSyncUnsafe()

      // then
      lastSeenConnectionId mustBe connection2.id
    }
  }
}
