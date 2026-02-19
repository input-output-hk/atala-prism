package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.postgres.sqlstate
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.node.models.StorageData.{Bytes, IpfsCid}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.StateError.{
  EntityExists,
  EntityMissing,
  IllegalSecp256k1Key,
  InvalidKeyUsed,
  InvalidPreviousOperation
}
import io.iohk.atala.prism.node.operations.ValidationError.InvalidValue
import io.iohk.atala.prism.node.operations.path._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, VdrEntriesDAO}
import io.iohk.atala.prism.protos.node_models
import scala.util.Try

sealed trait StorageOperation extends Operation {
  protected def vdrKeyForDid(didSuffix: DidSuffix, keyId: String): EitherT[ConnectionIO, StateError, SecpPublicKey] =
    for {
      normalizedKeyId <- EitherT.fromEither[ConnectionIO] {
        if (!keyId.contains("#")) {
          // backward compatibility: treat bare key ids as belonging to the DID of the current operation
          Right(keyId)
        } else {
          val Array(didPart, kidPart) = keyId.split("#", 2)
          val maybeSuffix =
            // prefer the suffix part of a fully qualified DID (did:prism:<suffix>)
            if (didPart.startsWith("did:")) DidSuffix.fromString(didPart.split(":").last).toOption
            else DidSuffix.fromString(didPart).toOption

          maybeSuffix match {
            case Some(ds) if ds == didSuffix => Right(kidPart)
            case _ => Left(EntityMissing("key", keyId): StateError)
          }
        }
      }
      secpKey <- EitherT[ConnectionIO, StateError, SecpPublicKey] {
        PublicKeysDAO
          .find(didSuffix, normalizedKeyId)
          .map(_.toRight(EntityMissing("key", keyId): StateError))
          .map(_.flatMap { state =>
            for {
              _ <- Either.cond(state.keyUsage == KeyUsage.VDRKey, (), InvalidKeyUsed("VDR signing key"))
              _ <- Either.cond(state.revokedOn.isEmpty, (), StateError.KeyAlreadyRevoked(): StateError)
              secp <- Try(SecpPublicKey.unsafeFromCompressed(state.key.compressedKey)).toEither
                .leftMap(_ => IllegalSecp256k1Key(state.keyId): StateError)
            } yield secp
          })
      }
    } yield secpKey

  protected def ensureDidExists(didSuffix: DidSuffix): EitherT[ConnectionIO, StateError, Unit] =
    EitherT {
      DIDDataDAO
        .getLastOperation(didSuffix)
        .map {
          case Some(_) => Right(())
          case None => Left(EntityMissing("did suffix", didSuffix.getValue): StateError)
        }
    }

  /** Fetch the current head for the chain identified by `previousEventHash`, ensuring it is ACTIVE and matches the
    * provided hash.
    */
  protected def resolveActiveHead(
      previousEventHash: Sha256Hash
  ): EitherT[ConnectionIO, StateError, (Sha256Hash, VdrEntriesDAO.VdrEntryRow)] =
    for {
      entryId <- EitherT.fromOptionF(
        VdrEntriesDAO.findRootOf(previousEventHash).map(_.orElse(Some(previousEventHash))),
        EntityMissing("vdr entry", previousEventHash.hexEncoded): StateError
      )
      headHash <- EitherT.fromOptionF(
        VdrEntriesDAO.findHead(entryId),
        EntityMissing("vdr entry", previousEventHash.hexEncoded): StateError
      )
      head <- EitherT.fromOptionF(
        VdrEntriesDAO.find(headHash._1),
        EntityMissing("vdr entry", previousEventHash.hexEncoded): StateError
      )
      _ <- EitherT.fromEither[ConnectionIO](
        Either.cond(head.status == VdrEntryStatus.ACTIVE, (), InvalidPreviousOperation(): StateError)
      )
      _ <- EitherT.fromEither[ConnectionIO](
        Either.cond(head.eventHash == previousEventHash, (), InvalidPreviousOperation(): StateError)
      )
    } yield (entryId, head)
}

final case class CreateStorageEntryOperation(
    didSuffix: DidSuffix,
    nonce: Option[Vector[Byte]],
    data: StorageData,
    digest: Sha256Hash,
    ledgerData: LedgerData
) extends StorageOperation {
  override val metricCounterName: String = "number_of_vdr_storage_entries_created"

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] =
    for {
      _ <- ensureDidExists(didSuffix)
      key <- vdrKeyForDid(didSuffix, keyId)
    } yield CorrectnessData(key, None)

  override protected def applyStateImpl(c: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] = {
    val (dataType, dataBytes, dataIpfs) = data match {
      case Bytes(value) => ("BYTES", Some(value.toArray), None)
      case IpfsCid(cid) => ("IPFS", None, Some(cid))
      case sle @ StorageData.StatusListEntry(_, _, _) =>
        ("STATUS_LIST", Some(sle.toString.getBytes), None)
    }
    for {
      _ <- EitherT {
        VdrEntriesDAO
          .insert(
            digest,
            digest,
            didSuffix,
            nonce.map(_.toArray),
            dataType,
            dataBytes,
            dataIpfs,
            None,
            VdrEntryStatus.ACTIVE,
            ledgerData
          )
          .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("vdr entry", digest.hexEncoded): StateError
          }
      }
      _ <- EitherT.right(
        VdrEntriesDAO.insertHead(digest, digest, VdrEntryStatus.ACTIVE).attemptSql
      )
    } yield ()
  }
}

final case class UpdateStorageEntryOperation(
    previousEventHash: Sha256Hash,
    data: StorageData,
    digest: Sha256Hash,
    ledgerData: LedgerData
) extends StorageOperation {
  override val metricCounterName: String = "number_of_vdr_storage_entries_updated"

  override def linkedPreviousOperation: Option[Sha256Hash] = Some(previousEventHash)

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] =
    for {
      head <- resolveActiveHead(previousEventHash).map(_._2)
      _ <- ensureDidExists(head.didSuffix)
      key <- vdrKeyForDid(head.didSuffix, keyId)
    } yield CorrectnessData(key, Some(previousEventHash))

  override protected def applyStateImpl(c: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] = {
    val (dataType, dataBytes, dataIpfs) = data match {
      case Bytes(value) => ("BYTES", Some(value.toArray), None)
      case IpfsCid(cid) => ("IPFS", None, Some(cid))
      case sle @ StorageData.StatusListEntry(_, _, _) =>
        ("STATUS_LIST", Some(sle.toString.getBytes), None)
    }
    for {
      resolved <- resolveActiveHead(previousEventHash)
      (entryId, head) = resolved
      _ <- EitherT(
        VdrEntriesDAO
          .insert(
            entryId,
            digest,
            head.didSuffix,
            None,
            dataType,
            dataBytes,
            dataIpfs,
            Some(previousEventHash),
            VdrEntryStatus.ACTIVE,
            ledgerData
          )
          .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("vdr entry", digest.hexEncoded): StateError
          }
      )
      _ <- EitherT.right(
        VdrEntriesDAO.updateHead(entryId, digest, VdrEntryStatus.ACTIVE).attemptSql
      )
    } yield ()
  }
}

final case class DeactivateStorageEntryOperation(
    previousEventHash: Sha256Hash,
    digest: Sha256Hash,
    ledgerData: LedgerData
) extends StorageOperation {
  override val metricCounterName: String = "number_of_vdr_storage_entries_deactivated"

  override def linkedPreviousOperation: Option[Sha256Hash] = Some(previousEventHash)

  override def getCorrectnessData(keyId: String): EitherT[ConnectionIO, StateError, CorrectnessData] =
    for {
      resolved <- resolveActiveHead(previousEventHash)
      (_, head) = resolved
      _ <- ensureDidExists(head.didSuffix)
      key <- vdrKeyForDid(head.didSuffix, keyId)
    } yield CorrectnessData(key, Some(previousEventHash))

  override protected def applyStateImpl(c: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] =
    for {
      resolved <- resolveActiveHead(previousEventHash)
      (entryId, head) = resolved
      _ <- EitherT(
        VdrEntriesDAO
          .insert(
            entryId,
            digest,
            head.didSuffix,
            None,
            dataType = "NONE",
            dataBytes = None,
            dataIpfs = None,
            Some(previousEventHash),
            VdrEntryStatus.DEACTIVATED,
            ledgerData
          )
          .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            EntityExists("vdr entry", digest.hexEncoded): StateError
          }
      )
      _ <- EitherT.right(
        VdrEntriesDAO.updateHead(entryId, digest, VdrEntryStatus.DEACTIVATED).attemptSql
      )
    } yield ()
}

object StorageOperations {

  private def parseStorageData(data: node_models.StorageData, path: Path): Either[ValidationError, StorageData] =
    data.content match {
      case node_models.StorageData.Content.Bytes(value) =>
        Right(Bytes(value.toByteArray.toVector))
      case node_models.StorageData.Content.Ipfs(cid) =>
        Right(IpfsCid(cid))
      case node_models.StorageData.Content.StatusListEntry(entry) =>
        Right(
          StorageData.StatusListEntry(
            entry.state,
            Option(entry.name).filter(_.nonEmpty),
            Option(entry.details).filter(_.nonEmpty)
          )
        )
      case node_models.StorageData.Content.Empty =>
        Left(InvalidValue(path / "data", "empty", "StorageData must be provided"))
    }

  def parseCreate(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, CreateStorageEntryOperation] = {
    val create = ValueAtPath(operation, Path.root).child(_.getCreateStorageEntry, "createStorageEntry")
    for {
      didHashBytes <- create.child(_.didPrismHash, "didPrismHash").parse { bytes =>
        Try(Sha256Hash.fromBytes(bytes.toByteArray)).toEither.leftMap(_ => "Invalid did_prism_hash")
      }
      dataProto <- create.childGet(_.data, "data")
      data <- parseStorageData(dataProto.value, dataProto.path)
      nonceBytes = create(_.nonce)
      nonce = if (nonceBytes.isEmpty) None else Some(nonceBytes.toByteArray.toVector)
      digest = Sha256Hash.compute(operation.toByteArray)
    } yield CreateStorageEntryOperation(DidSuffix(didHashBytes.hexEncoded), nonce, data, digest, ledgerData)
  }

  def parseUpdate(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, UpdateStorageEntryOperation] = {
    val update = ValueAtPath(operation, Path.root).child(_.getUpdateStorageEntry, "updateStorageEntry")
    for {
      prevHash <- update.child(_.previousEventHash, "previousEventHash").parse { bytes =>
        Try(Sha256Hash.fromBytes(bytes.toByteArray)).toEither.leftMap(_ => "Invalid previous_event_hash")
      }
      dataProto <- update.childGet(_.data, "data")
      data <- parseStorageData(dataProto.value, dataProto.path)
      digest = Sha256Hash.compute(operation.toByteArray)
    } yield UpdateStorageEntryOperation(prevHash, data, digest, ledgerData)
  }

  def parseDeactivate(
      operation: node_models.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, DeactivateStorageEntryOperation] = {
    val deactivate = ValueAtPath(operation, Path.root).child(_.getDeactivateStorageEntry, "deactivateStorageEntry")
    for {
      prevHash <- deactivate.child(_.previousEventHash, "previousEventHash").parse { bytes =>
        Try(Sha256Hash.fromBytes(bytes.toByteArray)).toEither.leftMap(_ => "Invalid previous_event_hash")
      }
      digest = Sha256Hash.compute(operation.toByteArray)
    } yield DeactivateStorageEntryOperation(prevHash, digest, ledgerData)
  }
}
