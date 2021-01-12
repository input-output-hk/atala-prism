package io.iohk.atala.prism.kycbridge

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import cats.effect.Sync
import doobie.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}

trait KycBridgeFixtures {

  def insertManyFixtures[F[_]: Sync, M](records: ConnectionIO[M]*)(database: Transactor[F]): F[Unit] =
    records.toList.sequence.transact(database).void

  object ConnectionFixtures {
    lazy val connectionId1: ConnectionId = ConnectionId(UUID.fromString("3a66fcef-4d50-4a67-a365-d4dbebcf22d3"))
    lazy val connectionId2: ConnectionId = ConnectionId(UUID.fromString("06325aef-d937-41b2-9a6c-b654e02b273d"))
    lazy val connection1: Connection =
      Connection(
        token = ConnectionToken("token1"),
        id = Some(connectionId1),
        state = ConnectionState.Invited,
        updatedAt = LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC),
        acuantDocumentInstanceId = None,
        acuantDocumentStatus = None
      )
    lazy val connection2: Connection =
      Connection(
        token = ConnectionToken("token2"),
        id = Some(connectionId2),
        state = ConnectionState.Invited,
        updatedAt = LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC),
        acuantDocumentInstanceId = Some(AcuantDocumentInstanceId("920dacc8-9d6d-4a11-aa02-c1dede4729cd")),
        acuantDocumentStatus = None
      )

    def insertAll[F[_]: Sync](database: Transactor[F]): F[Unit] = {
      insertManyFixtures(
        ConnectionDao.insert(connection1),
        ConnectionDao.insert(connection2)
      )(database)
    }
  }
}
