package io.iohk.atala.prism.node.repositories

import cats.Applicative
import cats.effect.MonadCancelThrow
import cats.effect.Resource
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.StorageData.{Bytes, IpfsCid}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.daos.VdrEntriesDAO
import io.iohk.atala.prism.node.repositories.daos.VdrEntriesDAO.VdrEntryRow
import io.iohk.atala.prism.node.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.protos.node_models
import java.nio.charset.StandardCharsets
import tofu.logging.Logs
import scala.util.Try

case class VdrEntry(
    eventHash: Sha256Hash,
    didSuffix: DidSuffix,
    data: Option[StorageData],
    previousEventHash: Option[Sha256Hash],
    status: VdrEntryStatus,
    nonce: Option[Array[Byte]]
)

final case class VdrEntryHead(
    entryId: Sha256Hash,
    latestHash: Sha256Hash,
    status: VdrEntryStatus
)

trait VdrEntriesRepository[F[_]] {
  def insertCreate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      nonce: Option[Array[Byte]],
      data: StorageData,
      ledgerData: LedgerData
  ): F[Unit]

  def insertUpdate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      previousEventHash: Sha256Hash,
      data: StorageData,
      ledgerData: LedgerData
  ): F[Unit]

  def insertDeactivate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      previousEventHash: Sha256Hash,
      ledgerData: LedgerData
  ): F[Unit]

  def find(eventHash: Sha256Hash): F[Option[VdrEntry]]

  def findLatest(entryId: Sha256Hash): F[Option[VdrEntry]]
}

object VdrEntriesRepository {
  def apply[F[_]: MonadCancelThrow, R[_]: Applicative](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[VdrEntriesRepository[F]] = {
    val _ = logs
    Applicative[R].pure(new VdrEntriesRepositoryImpl[F](transactor))
  }

  def resource[F[_]: MonadCancelThrow, R[_]: Applicative](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, VdrEntriesRepository[F]] =
    Resource.eval(apply(transactor, logs))
}

private final class VdrEntriesRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends VdrEntriesRepository[F] {

  private def toDbData(data: StorageData): (String, Option[Array[Byte]], Option[String]) = data match {
    case Bytes(value) => ("BYTES", Some(value.toArray), None)
    case IpfsCid(cid) => ("IPFS", None, Some(cid))
    case sle: StorageData.StatusListEntry =>
      ("STATUS_LIST", Some(toProtoStatusListBytes(sle)), None)
  }

  private def toProtoStatusListBytes(entry: StorageData.StatusListEntry): Array[Byte] =
    node_models
      .StatusListEntry(
        state = entry.state,
        name = entry.name.getOrElse(""),
        details = entry.details.getOrElse("")
      )
      .toByteArray

  private def fromProtoStatusListBytes(bytes: Array[Byte]): Option[StorageData.StatusListEntry] =
    Try(node_models.StatusListEntry.parseFrom(bytes)).toOption.map { entry =>
      StorageData.StatusListEntry(
        state = entry.state,
        name = Option(entry.name).filter(_.nonEmpty),
        details = Option(entry.details).filter(_.nonEmpty)
      )
    }

  private def fromDb(row: VdrEntryRow): Option[VdrEntry] = {
    val data = row.dataType match {
      case "BYTES" => row.dataBytes.map(bytes => Bytes(bytes.toVector))
      case "IPFS" => row.dataIpfs.map(IpfsCid.apply)
      case "STATUS_LIST" =>
        row.dataBytes.map { bytes =>
          fromProtoStatusListBytes(bytes).getOrElse {
            // Backward compatibility for legacy rows previously serialized via `toString`.
            val legacyString = new String(bytes, StandardCharsets.UTF_8)
            StorageData.StatusListEntry(0, Some(legacyString), None)
          }
        }
      case "NONE" => None
      case _ => None
    }
    Some(
      VdrEntry(
        row.eventHash,
        row.didSuffix,
        data,
        row.previousEventHash,
        row.status,
        row.nonce
      )
    )
  }

  override def insertCreate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      nonce: Option[Array[Byte]],
      data: StorageData,
      ledgerData: LedgerData
  ): F[Unit] = {
    val (dataType, dataBytes, dataIpfs) = toDbData(data)
    (for {
      _ <- VdrEntriesDAO
        .insert(
          eventHash,
          eventHash,
          didSuffix,
          nonce,
          dataType,
          dataBytes,
          dataIpfs,
          None,
          VdrEntryStatus.ACTIVE,
          ledgerData
        )
        .logSQLErrorsV2("insert vdr create")
      _ <- VdrEntriesDAO.insertHead(eventHash, eventHash, VdrEntryStatus.ACTIVE)
    } yield ()).transact(xa)
  }

  override def insertUpdate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      previousEventHash: Sha256Hash,
      data: StorageData,
      ledgerData: LedgerData
  ): F[Unit] = {
    val entryIdF = VdrEntriesDAO.findRootOf(previousEventHash).map(_.getOrElse(previousEventHash))
    val (dataType, dataBytes, dataIpfs) = toDbData(data)
    (for {
      entryId <- entryIdF
      _ <- VdrEntriesDAO
        .insert(
          entryId,
          eventHash,
          didSuffix,
          None,
          dataType,
          dataBytes,
          dataIpfs,
          Some(previousEventHash),
          VdrEntryStatus.ACTIVE,
          ledgerData
        )
        .logSQLErrorsV2("insert vdr update")
      _ <- VdrEntriesDAO.updateHead(entryId, eventHash, VdrEntryStatus.ACTIVE)
    } yield ()).transact(xa)
  }

  override def insertDeactivate(
      eventHash: Sha256Hash,
      didSuffix: DidSuffix,
      previousEventHash: Sha256Hash,
      ledgerData: LedgerData
  ): F[Unit] =
    (for {
      entryId <- VdrEntriesDAO.findRootOf(previousEventHash).map(_.getOrElse(previousEventHash))
      _ <- VdrEntriesDAO
        .insert(
          entryId,
          eventHash,
          didSuffix,
          None,
          dataType = "NONE",
          dataBytes = None,
          dataIpfs = None,
          previousEventHash = Some(previousEventHash),
          status = VdrEntryStatus.DEACTIVATED,
          ledgerData = ledgerData
        )
        .logSQLErrorsV2("insert vdr deactivate")
      _ <- VdrEntriesDAO.updateHead(entryId, eventHash, VdrEntryStatus.DEACTIVATED)
    } yield ()).transact(xa)

  override def find(eventHash: Sha256Hash): F[Option[VdrEntry]] =
    VdrEntriesDAO
      .find(eventHash)
      .map(_.flatMap(fromDb))
      .logSQLErrorsV2(s"find vdr entry hash=${eventHash.hexEncoded}")
      .transact(xa)

  override def findLatest(eventHash: Sha256Hash): F[Option[VdrEntry]] =
    VdrEntriesDAO
      .findHead(eventHash)
      .flatMap {
        case Some((latestHash, _)) => VdrEntriesDAO.find(latestHash)
        case None => Applicative[doobie.free.connection.ConnectionIO].pure(Option.empty[VdrEntryRow])
      }
      .map(_.flatMap(fromDb))
      .logSQLErrorsV2(s"find latest vdr entry root=${eventHash.hexEncoded}")
      .transact(xa)
}
