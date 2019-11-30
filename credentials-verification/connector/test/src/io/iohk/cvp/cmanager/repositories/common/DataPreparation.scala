package io.iohk.cvp.cmanager.repositories.common

import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.{daos => cmanagerDaos}
import io.iohk.connector.repositories.{daos => connectorDaos}
import doobie.implicits._
import io.iohk.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.cvp.models.ParticipantId

object DataPreparation {

  import cmanagerDaos._
  import connectorDaos._

  def createIssuer(name: String = "Issuer")(implicit database: Transactor[IO]): Issuer.Id = {
    val id = Issuer.Id(UUID.randomUUID())
    val did = "did:geud:issuer-x"
    sql"""
         |INSERT INTO issuers (issuer_id, name, did)
         |VALUES ($id, $name, $did)
         |""".stripMargin.update.run.transact(database).unsafeRunSync()

    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant = ParticipantInfo(ParticipantId(id.value), ParticipantType.Issuer, name, Option(did))
    sql"""
         |INSERT INTO participants (id, tpe, name, did)
         |VALUES (${participant.id}, ${participant.tpe}, ${participant.name}, ${participant.did})
       """.stripMargin.update.run.transact(database).unsafeRunSync()
    id
  }
}
