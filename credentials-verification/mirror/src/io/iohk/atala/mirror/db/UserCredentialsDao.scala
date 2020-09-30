package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.UserCredentials
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection.ConnectionToken

object UserCredentialsDao {

  def findBy(connectionToken: ConnectionToken): ConnectionIO[Option[UserCredentials]] = {
    sql"""
    | SELECT connection_token, raw_credential, issuers_did
    | FROM user_credentials
    | WHERE connection_token = $connectionToken
    """.stripMargin.query[UserCredentials].option
  }

  def insert(userCredentials: UserCredentials): ConnectionIO[Int] =
    insertMany.toUpdate0(userCredentials).run

  private val insertMany: Update[UserCredentials] = {
    Update[UserCredentials](
      "INSERT INTO user_credentials(connection_token, raw_credential, issuers_did) values (?, ?, ?)"
    )
  }

}
