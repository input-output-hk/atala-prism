package io.iohk.atala.prism.node.migrations

import doobie.implicits._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.node.repositories.daos._
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import doobie.implicits.legacy.instant._

import io.iohk.atala.prism.interop.toScalaSDK._

import java.time.Instant

class V19MigrationSpec extends PostgresMigrationSpec("db.migration.V19") with BaseDAO {

  private val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  private val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).get,
    Ledger.InMemory,
    dummyTimestampInfo
  )
  val didDigest = SHA256Digest.compute("test".getBytes())
  val didSuffix = DIDSuffix.unsafeFromDigest(didDigest.asScala)
  val didPublicKey: DIDPublicKey =
    DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, EC.generateKeyPair().getPublicKey)

  private def insertPublicKey(key: DIDPublicKey, ledgerData: LedgerData) = {
    val curveName = ECConfig.getCURVE_NAME
    val point = key.key.getCurvePoint

    val xBytes = point.xBytes()
    val yBytes = point.yBytes()

    val addedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, x, y,
         |   added_on, added_on_absn, added_on_osn,
         |   added_on_transaction_id, ledger)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $xBytes, $yBytes,
         |   ${addedOn.atalaBlockTimestamp}, ${addedOn.atalaBlockSequenceNumber}, ${addedOn.operationSequenceNumber},
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
        .toPublicKey(
          didPublicKey.key.getCurvePoint.xBytes(),
          didPublicKey.key.getCurvePoint.yBytes()
        )
        .asScala
        .getCompressed
      inDB mustBe expected
      inDB.length mustBe 33
    }
  )
}
