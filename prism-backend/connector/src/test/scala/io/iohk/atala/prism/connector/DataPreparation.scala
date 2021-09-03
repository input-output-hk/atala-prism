package io.iohk.atala.prism.connector

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.{daos => connectorDaos}
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.models.ParticipantId

object DataPreparation extends BaseDAO {

  import connectorDaos._

  def newDID(): PrismDid = {
    PrismDid.buildLongFormFromMasterKey(EC.generateKeyPair().getPublicKey).asCanonical()
  }

  def createIssuer(
      name: String = "Issuer",
      tag: String = "",
      publicKey: Option[ECPublicKey] = None,
      did: Option[PrismDid] = None
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
  )(implicit database: Transactor[IO]): Option[Sha256Digest] = {
    sql"""
         |SELECT issuance_operation_hash
         |FROM published_batches
         |WHERE batch_id = $batchId
         |""".stripMargin
      .query[Sha256Digest]
      .option
      .transact(database)
      .unsafeRunSync()
  }
}
