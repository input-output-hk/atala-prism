package io.iohk.cvp.cmanager.repositories.common

import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.daos
import doobie.implicits._

object DataPreparation {

  import daos._

  def createIssuer(name: String = "Issuer")(implicit database: Transactor[IO]): Issuer.Id = {
    val id = Issuer.Id(UUID.randomUUID())
    val did = "did:geud:issuer-x"
    sql"""
         |INSERT INTO issuers (issuer_id, name, did)
         |VALUES ($id, $name, $did)
         |""".stripMargin.update.run.transact(database).unsafeRunSync()
    id
  }
}
