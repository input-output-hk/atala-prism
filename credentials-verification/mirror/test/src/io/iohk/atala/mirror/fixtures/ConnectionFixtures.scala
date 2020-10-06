package io.iohk.atala.mirror.fixtures

import java.util.UUID

import cats.effect.Sync
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import doobie.implicits._

object ConnectionFixtures {

  val connectionId1: ConnectionId = ConnectionId(UUID.randomUUID())
  val connectionId2: ConnectionId = ConnectionId(UUID.randomUUID())
  val connection1: Connection = Connection(ConnectionToken("token1"), Some(connectionId1), ConnectionState.Invited)
  val connection2: Connection = Connection(ConnectionToken("token2"), Some(connectionId2), ConnectionState.Invited)

  def insertAllConnections[F[_]: Sync](database: Transactor[F]): F[Unit] =
    (for {
      _ <- ConnectionDao.insert(connection1)
      _ <- ConnectionDao.insert(connection2)
    } yield ()).transact(database)

}
