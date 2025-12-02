package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.Read
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.utils.syntax._
import cats.syntax.functor._

import java.time.Instant

object VdrEntriesDAO {

  case class VdrEntryRow(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      nonce: Option[Array[Byte]],
      dataType: String,
      dataBytes: Option[Array[Byte]],
      dataIpfs: Option[String],
      previousEventHash: Option[Sha256Hash],
      status: VdrEntryStatus,
      createdAt: Instant,
      createdAtAbsN: Int,
      createdAtOsN: Int,
      createdAtTxId: TransactionId,
      createdAtLedger: Ledger
  )

  def insert(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      nonce: Option[Array[Byte]],
      dataType: String,
      dataBytes: Option[Array[Byte]],
      dataIpfs: Option[String],
      previousEventHash: Option[Sha256Hash],
      status: VdrEntryStatus,
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val ts = ledgerData.timestampInfo
    sql"""INSERT INTO vdr_entries(
         | event_hash, did_suffix, nonce, data_type, data_bytes, data_ipfs, previous_event_hash, status,
         | created_at, created_at_absn, created_at_osn, created_at_tx_id, created_at_ledger
         |) VALUES (
         | ${eventHash.bytes}, $didSuffix, $nonce, $dataType, $dataBytes, $dataIpfs, $previousEventHash, $status,
         | ${ts.atalaBlockTimestamp.toInstant}, ${ts.atalaBlockSequenceNumber}, ${ts.operationSequenceNumber},
         | ${ledgerData.transactionId}, ${ledgerData.ledger}
         |)
         |""".stripMargin.update.run.void
  }

  private implicit val vdrEntryRead: Read[VdrEntryRow] = {
    Read[
      (
          Sha256Hash,
          DidSuffix,
          Option[Array[Byte]],
          String,
          Option[Array[Byte]],
          Option[String],
          Option[Sha256Hash],
          VdrEntryStatus,
          Instant,
          Int,
          Int,
          TransactionId,
          Ledger
      )
    ].map { case (hash, did, nonce, dataType, dataBytes, dataIpfs, prevHash, status, ts, absn, osn, txId, ledger) =>
      VdrEntryRow(hash, did, nonce, dataType, dataBytes, dataIpfs, prevHash, status, ts, absn, osn, txId, ledger)
    }
  }

  def find(eventHash: Sha256Hash): ConnectionIO[Option[VdrEntryRow]] =
    sql"""SELECT event_hash, did_suffix, nonce, data_type, data_bytes, data_ipfs, previous_event_hash, status,
          |       created_at, created_at_absn, created_at_osn, created_at_tx_id, created_at_ledger
          |FROM vdr_entries
          |WHERE event_hash = ${eventHash}
          |""".stripMargin.query[VdrEntryRow].option
}
