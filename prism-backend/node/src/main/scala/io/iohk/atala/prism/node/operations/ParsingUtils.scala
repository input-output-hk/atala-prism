package io.iohk.atala.prism.node.operations

import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.crypto.{EC}
import io.iohk.atala.prism.kotlin.crypto.keys.{ECPublicKey}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.node.models.{DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.atala.prism.node.operations.path.ValueAtPath
import io.iohk.atala.prism.protos.{common_models, node_models}

import scala.util.Try

object ParsingUtils {

  def parseDate(date: ValueAtPath[common_models.Date]): Either[ValidationError, LocalDate] = {
    for {
      year <- date.child(_.year, "year").parse { year =>
        Either.cond(year > 0, year, "Year needs to be specified as positive value")
      }
      month <- date.child(_.month, "month").parse { month =>
        Either.cond(month >= 1 && month <= 12, month, "Month has to be specified and between 1 and 12")
      }
      parsedDate <- date.child(_.day, "day").parse { day =>
        Try(LocalDate.of(year, month, day)).toEither.left
          .map(_ => "Day has to be specified and a proper day in the month")
      }
    } yield parsedDate
  }
  val KEY_ID_RE = "^\\w+$".r

  def parseECKey(ecData: ValueAtPath[node_models.ECKeyData]): Either[ValidationError, ECPublicKey] = {
    if (ecData(_.curve) != ECConfig.getCURVE_NAME) {
      Left(ecData.child(_.curve, "curve").invalid("Unsupported curve"))
    } else if (ecData(_.x.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "x").missing())
    } else if (ecData(_.y.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "y").missing())
    } else {
      Try(EC.toPublicKey(ecData(_.x.toByteArray), ecData(_.y.toByteArray))).toEither.left
        .map(ex => InvalidValue(ecData.path, "", s"Unable to initialize the key: ${ex.getMessage}"))
    }
  }

  def parseKeyId(id: ValueAtPath[String]): Either[ValidationError, String] = {
    id.parse { id =>
      Either.cond(KEY_ID_RE.pattern.matcher(id).matches(), id, s"Invalid key id: $id")
    }
  }

  def parseKey(key: ValueAtPath[node_models.PublicKey], didSuffix: DIDSuffix): Either[ValidationError, DIDPublicKey] = {
    for {
      keyUsage <- key.child(_.usage, "usage").parse {
        case node_models.KeyUsage.MASTER_KEY => Right(KeyUsage.MasterKey)
        case node_models.KeyUsage.ISSUING_KEY => Right(KeyUsage.IssuingKey)
        case node_models.KeyUsage.COMMUNICATION_KEY => Right(KeyUsage.CommunicationKey)
        case node_models.KeyUsage.REVOCATION_KEY => Right(KeyUsage.RevocationKey)
        case node_models.KeyUsage.AUTHENTICATION_KEY => Right(KeyUsage.AuthenticationKey)
        case _ => Left("Unknown value")
      }
      keyId <- parseKeyId(key.child(_.id, "id"))
      _ <- Either.cond(key(_.keyData.isDefined), (), MissingValue(key.path / "keyData"))
      publicKey <- parseECKey(key.child(_.getEcKeyData, "ecKeyData"))
    } yield DIDPublicKey(didSuffix, keyId, keyUsage, publicKey)
  }

  def parseHash(hash: ValueAtPath[ByteString]): Either[ValidationError, SHA256Digest] = {
    hash.parse { hash =>
      Either.cond(
        hash.size() == SHA256Digest.getBYTE_LENGTH,
        SHA256Digest.fromBytes(hash.toByteArray),
        s"must have ${SHA256Digest.getBYTE_LENGTH} bytes"
      )
    }
  }

  def parseHashList(hashesV: ValueAtPath[Seq[ByteString]]): Either[ValidationError, List[SHA256Digest]] = {
    hashesV.parse { hashes =>
      Either.cond(
        hashes.forall(_.size() == SHA256Digest.getBYTE_LENGTH),
        hashes.map(h => SHA256Digest.fromBytes(h.toByteArray)).to(List),
        s"must have ${SHA256Digest.getBYTE_LENGTH} bytes"
      )
    }
  }
}
