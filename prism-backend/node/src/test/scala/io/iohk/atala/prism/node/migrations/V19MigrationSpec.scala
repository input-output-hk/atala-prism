package io.iohk.atala.prism.node.migrations

import doobie.implicits._
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.models.{DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.node.repositories.daos._
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.protos.models.TimestampInfo

import java.time.Instant

class V19MigrationSpec extends PostgresMigrationSpec("db.migration.V19") with BaseDAO {

  private val dummyTimestampInfo =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .get,
    Ledger.InMemory,
    dummyTimestampInfo
  )
  val didDigest = Sha256.compute("test".getBytes())
  val didSuffix = DidSuffix(didDigest.getHexValue)
  val didPublicKey: DIDPublicKey =
    DIDPublicKey(
      didSuffix,
      "master",
      KeyUsage.MasterKey,
      EC.generateKeyPair().getPublicKey
    )

  private def insertPublicKey(key: DIDPublicKey, ledgerData: LedgerData) = {
    val curveName = ECConfig.getCURVE_NAME
    val point = key.key.getCurvePoint

    val xBytes = point.getX.bytes()
    val yBytes = point.getY.bytes()

    val addedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, x, y,
         |   added_on, added_on_absn, added_on_osn,
         |   added_on_transaction_id, ledger)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $xBytes, $yBytes,
         |   ${Instant
      .ofEpochMilli(addedOn.getAtalaBlockTimestamp)}, ${addedOn.getAtalaBlockSequenceNumber}, ${addedOn.getOperationSequenceNumber},
         |   ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.runUpdate()
  }

  private def selectPublicKeyCompressed(key: DIDPublicKey) = {
    sql"SELECT xCompressed FROM public_keys WHERE did_suffix = ${key.didSuffix} AND key_id = ${key.keyId}"
      .runUnique[Array[Byte]]()
  }

  test(
    beforeApply = {
      insertPublicKey(didPublicKey, dummyLedgerData)
    },
    afterApplied = {
      val inDB = selectPublicKeyCompressed(didPublicKey)
      val expected = EC
        .toPublicKeyFromByteCoordinates(
          didPublicKey.key.getCurvePoint.getX.bytes(),
          didPublicKey.key.getCurvePoint.getY.bytes()
        )
        .getEncodedCompressed
      inDB mustBe expected
      inDB.length mustBe 33
    }
  )
}
