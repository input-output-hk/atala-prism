package io.iohk.cvp.cstore.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cstore.models.StoreUser
import io.iohk.cvp.models.ParticipantId

object StoreUsersDAO {
  def insert(user: StoreUser): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO verifiers (user_id)
         |VALUES (${user.id});
       """.stripMargin.update.run.map(_ => ())
  }

  def get(userId: ParticipantId): ConnectionIO[Option[StoreUser]] = {
    sql"""
         |SELECT user_id
         |FROM verifiers
         |WHERE user_id = $userId
       """.stripMargin.query[StoreUser].option
  }
}
