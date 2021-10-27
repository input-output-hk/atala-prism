package io.iohk.atala.prism.connector.repositories.daos

import cats.implicits._
import doobie.FC
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.update.Update
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

  def insert(
      initiator: ParticipantId,
      tokens: List[TokenString]
  ): doobie.ConnectionIO[Unit] = {
    Update[(TokenString, ParticipantId)]("""
         |INSERT INTO connection_tokens (token, initiator)
         |VALUES (?, ?)""".stripMargin)
      .updateMany(tokens.map(token => token -> initiator))
      .flatTap { affectedRows =>
        FC.raiseError(
          new RuntimeException(
            s"Unknown error while inserting ${tokens.size} tokens"
          )
        ).whenA(tokens.size != affectedRows)
      }
      .void
  }

  def markAsUsed(token: TokenString): doobie.ConnectionIO[Int] = {
    sql"""
         |UPDATE connection_tokens
         |SET used_at = ${Instant.now()}
         |WHERE token=$token""".stripMargin.update.run
  }
}
