package io.iohk.atala.prism.kycbridge.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.kycbridge.models.Connection
import java.time.Instant

object ConnectionDao {

  def findByConnectionToken(token: ConnectionToken): ConnectionIO[Option[Connection]] = {
    sql"""
         | SELECT token, id, state, updated_at, acuant_document_instance_id, acuant_document_status
         | FROM connections
         | WHERE token = $token
    """.stripMargin.query[Connection].option
  }

  def findByConnectionId(id: ConnectionId): ConnectionIO[Option[Connection]] = {
    sql"""
         | SELECT token, id, state, updated_at, acuant_document_instance_id, acuant_document_status
         | FROM connections
         | WHERE id = $id
    """.stripMargin.query[Connection].option
  }

  val findLastSeenConnectionId: ConnectionIO[Option[ConnectionId]] = {
    sql"""
         | SELECT id
         | FROM connections
         | WHERE id IS NOT NULL
         | ORDER BY updated_at DESC, id DESC
         | LIMIT 1
    """.stripMargin.query[ConnectionId].option
  }

  val findConnectionWithoutDocumentId: ConnectionIO[Option[Connection]] = {
    sql"""
         | SELECT token, id, state, updated_at, acuant_document_instance_id, acuant_document_status
         | FROM connections
         | WHERE
         | id IS NOT NULL
         | AND acuant_document_instance_id IS NULL
         | ORDER BY updated_at, id
         | LIMIT 1
    """.stripMargin.query[Connection].option
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
         | updated_at = ${Instant.now()},
         | acuant_document_instance_id = ${connection.acuantDocumentInstanceId},
         | acuant_document_status = ${connection.acuantDocumentStatus}::ACUANT_DOCUMENT_STATUS
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
    Update[Connection](
      """
        | INSERT INTO connections(
        | token,
        | id,
        | state,
        | updated_at,
        | acuant_document_instance_id,
        | acuant_document_status)
        | values (?, ?, ?::CONNECTION_STATE, ?, ?, ?::ACUANT_DOCUMENT_STATUS)""".stripMargin
    )

}
