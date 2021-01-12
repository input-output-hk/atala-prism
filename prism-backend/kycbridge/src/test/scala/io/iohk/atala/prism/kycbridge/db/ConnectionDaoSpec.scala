package io.iohk.atala.prism.kycbridge.db

import java.util.UUID

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken}
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId

// sbt "project kycbridge" "testOnly *db.ConnectionDaoSpec"
class ConnectionDaoSpec extends PostgresRepositorySpec with KycBridgeFixtures {
  import ConnectionFixtures._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "ConnectionDao" should {
    "insert single connection into the db" in {
      ConnectionDao
        .insert(connection1)
        .transact(database)
        .unsafeRunSync() mustBe 1
    }

    "update connection row" in {
      val connectionWithNewId = connection1.copy(id = connection2.id)
      (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- ConnectionDao.update(connectionWithNewId)
        connection <- ConnectionDao.findByConnectionToken(connection1.token)
      } yield connection)
        .transact(database)
        .unsafeRunSync()
        //ignore updated at
        .map(_.copy(updatedAt = connectionWithNewId.updatedAt)) mustBe Some(connectionWithNewId)
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

    "return none if a token doesn't exist" in {
      ConnectionDao.findByConnectionToken(ConnectionToken("token")).transact(database).unsafeRunSync() mustBe None
    }

    "return none if a connection id doesn't exist" in {
      ConnectionDao.findByConnectionId(ConnectionId(UUID.randomUUID())).transact(database).unsafeRunSync() mustBe None
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

    "return connection without document instance id" in {
      // given
      ConnectionFixtures.insertAll(database).unsafeRunSync()

      // when
      val connection: Option[Connection] =
        ConnectionDao.findConnectionWithoutDocumentId.transact(database).unsafeRunSync()

      // then
      connection mustBe Some(connection1)
    }

    "do not return connection with document instance id and bearer token" in {
      // given
      ConnectionFixtures.insertAll(database).unsafeRunSync()
      ConnectionDao
        .update(
          connection1.copy(
            acuantDocumentInstanceId = Some(AcuantDocumentInstanceId("920dacc8-9d6d-4a11-aa02-c1dede4729cd"))
          )
        )
        .transact(database)
        .unsafeRunSync()

      // when
      val connection: Option[Connection] =
        ConnectionDao.findConnectionWithoutDocumentId.transact(database).unsafeRunSync()

      // then
      connection mustBe None
    }
  }
}
