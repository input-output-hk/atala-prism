package io.iohk.atala.prism.connector

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.{daos => connectorDaos}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.OptionValues._

object DataPreparation extends BaseDAO {

  import connectorDaos._

  def newDID(): DID = {
    DID.createUnpublishedDID(EC.generateKeyPair().publicKey).canonical.value
  }

  def createIssuer(
      name: String = "Issuer",
      tag: String = "",
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = ParticipantId.random()
    val didValue = did.getOrElse(newDID())
    // dirty hack to create a participant while creating an issuer, TODO: Merge the tables
    val participant = ParticipantInfo(
      id,
      ParticipantType.Issuer,
      publicKey,
      name + tag,
      Option(didValue),
      None,
      None
    )
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  def createVerifier(
      id: ParticipantId = ParticipantId.random(),
      name: String = "Verifier",
      tag: String = "",
      publicKey: Option[ECPublicKey] = None
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val participant =
      ParticipantInfo(id, ParticipantType.Verifier, publicKey, name + tag, Option(newDID()), None, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  def getBatchData(
      batchId: CredentialBatchId
  )(implicit database: Transactor[IO]): Option[SHA256Digest] = {
    sql"""
         |SELECT issuance_operation_hash
         |FROM published_batches
         |WHERE batch_id = $batchId
         |""".stripMargin
      .query[SHA256Digest]
      .option
      .transact(database)
      .unsafeRunSync()
  }
}
