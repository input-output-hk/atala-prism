package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.UserCredential
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection.ConnectionToken

object UserCredentialDao {

  def findBy(connectionToken: ConnectionToken): ConnectionIO[Option[UserCredential]] = {
    sql"""
    | SELECT connection_token, raw_credential, issuers_did
    | FROM user_credentials
    | WHERE connection_token = $connectionToken
    """.stripMargin.query[UserCredential].option
  }

  def insert(userCredential: UserCredential): ConnectionIO[Int] =
    insertMany.toUpdate0(userCredential).run

  private val insertMany: Update[UserCredential] = {
    Update[UserCredential](
      "INSERT INTO user_credentials(connection_token, raw_credential, issuers_did) values (?, ?, ?)"
    )
  }

}
