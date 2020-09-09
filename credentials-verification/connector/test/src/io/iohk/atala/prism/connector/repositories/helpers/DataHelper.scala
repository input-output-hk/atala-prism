package io.iohk.atala.prism.connector.repositories.helpers

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.IssuerGroupsRepository
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import org.scalatest.EitherValues._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext

object DataHelper {
  def createParticipant(
      tpe: ParticipantType,
      name: String,
      did: String,
      publicKey: Option[ECPublicKey],
      logo: Option[ParticipantLogo]
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo) VALUES
          (${ParticipantId.random()}, $tpe, $did, $publicKey, $name, $logo)
          RETURNING id"""
      .runUnique[ParticipantId]()
  }

  def createIssuer(id: ParticipantId)(implicit
      database: Transactor[IO]
  ): Unit = {
    val _ = sql"""INSERT INTO issuers(issuer_id) VALUES($id)"""
      .runUpdate()
  }

  def createGroup(issuer: ParticipantId, name: String)(implicit
      database: Transactor[IO],
      ec: ExecutionContext
  ): IssuerGroup = {
    val issuerGroupsRepository = new IssuerGroupsRepository(database)
    issuerGroupsRepository.create(Issuer.Id(issuer.uuid), IssuerGroup.Name(name)).value.futureValue.right.value
  }

  def createIssuer(name: String = "Issuer", logo: Option[ParticipantLogo] = None)(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val pid = createParticipant(ParticipantType.Issuer, name, s"did:test:${name.toLowerCase}", None, logo)
    createIssuer(pid)
    pid
  }
}
