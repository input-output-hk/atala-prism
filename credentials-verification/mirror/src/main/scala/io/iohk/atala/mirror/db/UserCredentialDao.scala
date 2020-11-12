package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{ConnectorMessageId, UserCredential}
import io.iohk.atala.prism.daos.BaseDAO.didMeta
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.mirror.models.Connection.ConnectionToken

object UserCredentialDao {

  def findBy(connectionToken: ConnectionToken): ConnectionIO[List[UserCredential]] = {
    sql"""
    | SELECT connection_token, raw_credential, issuers_did, message_id, message_received_date, status
    | FROM user_credentials
    | WHERE connection_token = $connectionToken
    """.stripMargin.query[UserCredential].to[List]
  }

  def insert(userCredential: UserCredential): ConnectionIO[Int] =
    insertMany.toUpdate0(userCredential).run

  val findLastSeenMessageId: ConnectionIO[Option[ConnectorMessageId]] =
    sql"""
    | SELECT message_id
    | FROM user_credentials
    | ORDER BY message_received_date DESC
    | LIMIT 1
    """.stripMargin.query[ConnectorMessageId].option

  val insertMany: Update[UserCredential] =
    Update[UserCredential](
      """INSERT INTO
        | user_credentials(connection_token, raw_credential, issuers_did, message_id, message_received_date, status)
        | values (?, ?, ?, ?, ?, ?::CREDENTIAL_STATUS)
        | ON CONFLICT DO NOTHING""".stripMargin
    )

}
