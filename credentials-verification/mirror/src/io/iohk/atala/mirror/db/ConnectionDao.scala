package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO

import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}

import cats.data.NonEmptyList
import doobie.Fragments

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

object ConnectionDao {

  def findBy(token: ConnectionToken): ConnectionIO[Option[Connection]] = {
    sql"""
    | SELECT token, id, state
    | FROM connections
    | WHERE token = $token
    """.stripMargin.query[Connection].option
  }

  def findBy(id: ConnectionId): ConnectionIO[Option[Connection]] = {
    sql"""
    | SELECT token, id, state
    | FROM connections
    | WHERE id = $id
    """.stripMargin.query[Connection].option
  }

  def findBy(ids: NonEmptyList[ConnectionId]): ConnectionIO[List[Connection]] = {
    val sql =
      fr"""
          | SELECT token, id, state
          | FROM connections
          | WHERE
      """.stripMargin ++ Fragments.in(fr"id", ids)

    sql.query[Connection].to[List]
  }

  /**
    * Insert connection into the db.
    *
    * @return returns 1 if the record has been added
    */
  def insert(connection: Connection): ConnectionIO[Int] =
    insertMany.toUpdate0(connection).run

  /**
    * Insert many [[Conection]] rows with:
    *
    * {{{
    *   import cats.implicits._ // import to provide [[cats.Foldable]] for [[List]]
    *   insertMany.updateMany(List(Connection(...)))
    * }}}
    */
  private def insertMany: Update[Connection] =
    Update[Connection]("INSERT INTO connections(token, id, state) values (?, ?, ?::CONNECTION_STATE)")

}
