package io.iohk.cvp.cstore.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cstore.models.Verifier

object VerifiersDAO {
  def insert(verifier: Verifier): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO verifiers (verifier_id)
         |VALUES (${verifier.id})
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(verifierId: Verifier.Id): ConnectionIO[Option[Verifier]] = {
    sql"""
         |SELECT verifier_id
         |FROM verifiers
         |WHERE verifier_id = $verifierId
       """.stripMargin.query[Verifier].option
  }
}
