package io.iohk.atala.prism.connector.repositories.daos

import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.connector.model._
import java.time.Instant

object ConnectionTokensDAO {
  def exists(tokenString: TokenString): doobie.ConnectionIO[Boolean] = {
    sql"""
         |SELECT 1 FROM connection_tokens WHERE token = $tokenString
       """.stripMargin
      .query[Int]
      .option
      .map(_.isDefined)
  }

  def insert(initiator: ParticipantId, token: TokenString): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO connection_tokens (token, initiator)
         |VALUES ($token, $initiator)""".stripMargin.update.run
  }

  def markAsUsed(token: TokenString): doobie.ConnectionIO[Int] = {
    sql"""
         |UPDATE connection_tokens
         |SET used_at = ${Instant.now()}
         |WHERE token=$token""".stripMargin.update.run
  }
}
