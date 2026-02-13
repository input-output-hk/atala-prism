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

  def insertHead(entryId: Sha256Hash, latestHash: Sha256Hash, status: VdrEntryStatus): ConnectionIO[Unit] =
    sql"""INSERT INTO vdr_entry_heads(entry_id, latest_hash, status)
          |VALUES (${entryId}, ${latestHash}, ${status})
          |ON CONFLICT (entry_id) DO UPDATE SET latest_hash = EXCLUDED.latest_hash, status = EXCLUDED.status, updated_at = NOW()
          |""".stripMargin.update.run.void

  def updateHead(entryId: Sha256Hash, latestHash: Sha256Hash, status: VdrEntryStatus): ConnectionIO[Unit] =
    sql"""UPDATE vdr_entry_heads
          |SET latest_hash = ${latestHash}, status = ${status}, updated_at = NOW()
          |WHERE entry_id = ${entryId}
          |""".stripMargin.update.run.void

  def findHead(entryId: Sha256Hash): ConnectionIO[Option[(Sha256Hash, VdrEntryStatus)]] =
    sql"""SELECT latest_hash, status FROM vdr_entry_heads WHERE entry_id = ${entryId}""".stripMargin
      .query[(Sha256Hash, VdrEntryStatus)]
      .option

  /** Walk the chain from a given event hash forward and return the latest event (by created_at). */
  def findLatestFrom(root: Sha256Hash): ConnectionIO[Option[VdrEntryRow]] =
    sql"""
      WITH RECURSIVE chain AS (
        SELECT event_hash, did_suffix, nonce, data_type, data_bytes, data_ipfs, previous_event_hash, status,
               created_at, created_at_absn, created_at_osn, created_at_tx_id, created_at_ledger
        FROM vdr_entries
        WHERE event_hash = ${root}
        UNION ALL
        SELECT e.event_hash, e.did_suffix, e.nonce, e.data_type, e.data_bytes, e.data_ipfs, e.previous_event_hash, e.status,
               e.created_at, e.created_at_absn, e.created_at_osn, e.created_at_tx_id, e.created_at_ledger
        FROM vdr_entries e
        JOIN chain c ON e.previous_event_hash = c.event_hash
      )
      SELECT event_hash, did_suffix, nonce, data_type, data_bytes, data_ipfs, previous_event_hash, status,
             created_at, created_at_absn, created_at_osn, created_at_tx_id, created_at_ledger
      FROM chain
      ORDER BY created_at DESC, created_at_absn DESC, created_at_osn DESC
      LIMIT 1
      """.stripMargin.query[VdrEntryRow].option

  /** Walk backwards to the root (the first entry for an entry_id). */
  def findRootOf(hash: Sha256Hash): ConnectionIO[Option[Sha256Hash]] =
    sql"""
      WITH RECURSIVE parents AS (
        SELECT event_hash, previous_event_hash
        FROM vdr_entries
        WHERE event_hash = ${hash}
        UNION ALL
        SELECT e.event_hash, e.previous_event_hash
        FROM vdr_entries e
        JOIN parents p ON e.event_hash = p.previous_event_hash
      )
      SELECT event_hash
      FROM parents
      WHERE previous_event_hash IS NULL
      LIMIT 1
      """.stripMargin.query[Sha256Hash].option
}
