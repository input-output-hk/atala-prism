package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{Connection, DID}
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import cats.data.NonEmptyList
import doobie.Fragments
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

object ConnectionDao {

  def findByConnectionToken(token: ConnectionToken): ConnectionIO[Option[Connection]] = {
    sql"""
    | SELECT token, id, state, holder_did
    | FROM connections
    | WHERE token = $token
    """.stripMargin.query[Connection].option
  }

  def findByConnectionId(id: ConnectionId): ConnectionIO[Option[Connection]] = {
    sql"""
    | SELECT token, id, state, holder_did
    | FROM connections
    | WHERE id = $id
    """.stripMargin.query[Connection].option
  }

  def findByHolderDID(holderDID: DID): ConnectionIO[Option[Connection]] = {
    sql"""
    | SELECT token, id, state, holder_did
    | FROM connections
    | WHERE holder_did = $holderDID
    """.stripMargin.query[Connection].option
  }

  def findBy(ids: NonEmptyList[ConnectionId]): ConnectionIO[List[Connection]] = {
    val sql =
      fr"""
      | SELECT token, id, state, holder_did
      | FROM connections
      | WHERE
      """.stripMargin ++ Fragments.in(fr"id", ids)

    sql.query[Connection].to[List]
  }

  val findLastSeenConnectionId: ConnectionIO[Option[ConnectionId]] = {
    sql"""
    | SELECT id
    | FROM connections
    | WHERE id IS NOT NULL
    | ORDER BY updated_at, id DESC
    | LIMIT 1
    """.stripMargin.query[ConnectionId].option
  }

  /**
    * Insert connection into the db.
    *
    * @return returns 1 if the record has been added
    */
  def insert(connection: Connection): ConnectionIO[Int] =
    insertMany.toUpdate0(connection).run

  /**
    * Update connection by token.
    */
  def update(connection: Connection): ConnectionIO[Int] =
    sql"""
    | UPDATE connections SET
    | id = ${connection.id},
    | state = ${connection.state}::CONNECTION_STATE,
    | updated_at = now(),
    | holder_did = ${connection.holderDID}
    | WHERE token = ${connection.token}
    """.stripMargin.update.run

  /**
    * Insert many [[Conection]] rows with:
    *
    * {{{
    *   import cats.implicits._ // import to provide [[cats.Foldable]] for [[List]]
    *   insertMany.updateMany(List(Connection(...)))
    * }}}
    */
  private def insertMany: Update[Connection] =
    Update[Connection]("INSERT INTO connections(token, id, state, holder_did) values (?, ?, ?::CONNECTION_STATE, ?)")

}
