package io.iohk.node.operations

import java.security.PublicKey
import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.models.{DIDPublicKey, DIDSuffix, KeyUsage, SHA256Digest}
import io.iohk.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.node.operations.path.ValueAtPath
import io.iohk.node.{geud_node => proto}

import scala.util.Try

object ParsingUtils {

  def parseDate(date: ValueAtPath[proto.Date]): Either[ValidationError, LocalDate] = {
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

  def parseECKey(ecData: ValueAtPath[proto.ECKeyData]): Either[ValidationError, PublicKey] = {
    if (ecData(_.curve) != ECKeys.CURVE_NAME) {
      Left(ecData.child(_.curve, "curve").invalid("Unsupported curve"))
    } else if (ecData(_.x.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "x").missing())
    } else if (ecData(_.y.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "y").missing())
    } else {
      Try(ECKeys.toPublicKey(ecData(_.x.toByteArray), ecData(_.y.toByteArray))).toEither.left
        .map(ex => InvalidValue(ecData.path, "", s"Unable to initialize the key: ${ex.getMessage}"))
    }
  }

  def parseKeyId(id: ValueAtPath[String]): Either[ValidationError, String] = {
    id.parse { id =>
      Either.cond(KEY_ID_RE.pattern.matcher(id).matches(), id, "Invalid key id")
    }
  }

  def parseKey(key: ValueAtPath[proto.PublicKey], didSuffix: DIDSuffix): Either[ValidationError, DIDPublicKey] = {
    for {
      keyUsage <- key.child(_.usage, "usage").parse {
        case proto.KeyUsage.MASTER_KEY => Right(KeyUsage.MasterKey)
        case proto.KeyUsage.ISSUING_KEY => Right(KeyUsage.IssuingKey)
        case proto.KeyUsage.AUTHENTICATION_KEY => Right(KeyUsage.AuthenticationKey)
        case proto.KeyUsage.COMMUNICATION_KEY => Right(KeyUsage.CommunicationKey)
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
        hash.size() == SHA256Digest.BYTE_LENGTH,
        SHA256Digest(hash.toByteArray),
        s"mush have ${SHA256Digest.BYTE_LENGTH} bytes"
      )
    }
  }

}
