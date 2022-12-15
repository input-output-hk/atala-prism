package io.iohk.atala.prism.node.operations

import java.time.LocalDate
import cats.implicits._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, DIDServiceEndpoint, KeyUsage}
import io.iohk.atala.prism.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.atala.prism.node.operations.path.ValueAtPath
import io.iohk.atala.prism.protos.{common_models, node_models}

import scala.util.Try

object ParsingUtils {

  def parseDate(
      date: ValueAtPath[common_models.Date]
  ): Either[ValidationError, LocalDate] = {
    for {
      year <- date.child(_.year, "year").parse { year =>
        Either
          .cond(year > 0, year, "Year needs to be specified as positive value")
      }
      month <- date.child(_.month, "month").parse { month =>
        Either.cond(
          month >= 1 && month <= 12,
          month,
          "Month has to be specified and between 1 and 12"
        )
      }
      parsedDate <- date.child(_.day, "day").parse { day =>
        Try(LocalDate.of(year, month, day)).toEither.left
          .map(_ => "Day has to be specified and a proper day in the month")
      }
    } yield parsedDate
  }
  val KEY_ID_RE = "^\\w+$".r

  def parseKeyData(
      keyData: ValueAtPath[node_models.PublicKey]
  ): Either[ValidationError, ECPublicKey] = {
    if (keyData(_.keyData.isEcKeyData)) {
      parseECKey(keyData.child(_.getEcKeyData, "ecKeyData"))
    } else if (keyData(_.keyData.isCompressedEcKeyData)) {
      parseCompressedECKey(
        keyData.child(_.getCompressedEcKeyData, "compressedEcKeyData")
      )
    } else {
      Left(keyData.child(_.keyData, "keyData").missing())
    }
  }

  def parseECKey(
      ecData: ValueAtPath[node_models.ECKeyData]
  ): Either[ValidationError, ECPublicKey] = {
    if (ecData(_.curve) != ECConfig.getCURVE_NAME) {
      Left(ecData.child(_.curve, "curve").invalid("Unsupported curve"))
    } else if (ecData(_.x.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "x").missing())
    } else if (ecData(_.y.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "y").missing())
    } else {
      Try(
        EC.toPublicKeyFromByteCoordinates(
          ecData(_.x.toByteArray),
          ecData(_.y.toByteArray)
        )
      ).toEither.left
        .map(ex =>
          InvalidValue(
            ecData.path,
            "",
            s"Unable to initialize the key: ${ex.getMessage}"
          )
        )
    }
  }

  def parseCompressedECKey(
      ecData: ValueAtPath[node_models.CompressedECKeyData]
  ): Either[ValidationError, ECPublicKey] = {
    if (ecData(_.data.toByteArray.isEmpty)) {
      Left(ecData.child(_.data, "compressedData").missing())
    } else {
      Try(
        EC.toPublicKeyFromCompressed(ecData(_.data.toByteArray))
      ).toEither.left
        .map(ex =>
          InvalidValue(
            ecData.path,
            "",
            s"Unable to initialize the key: ${ex.getMessage}"
          )
        )
    }
  }

  def parseKeyId(id: ValueAtPath[String]): Either[ValidationError, String] = {
    id.parse { id =>
      Either.cond(
        KEY_ID_RE.pattern.matcher(id).matches(),
        id,
        s"Invalid key id: $id"
      )
    }
  }

  def parseServiceId(
      serviceId: ValueAtPath[String]
  ): Either[ValidationError, String] =
    serviceId.parse { id =>
      Either.cond(
        isValidUri(id),
        id,
        s"Id $id is not a valid URI"
      )
    }

  def parseServiceEndpoints(
      serviceEndpoints: ValueAtPath[List[String]],
      serviceId: String,
      canBeEmpty: Boolean = true
  ): Either[ValidationError, List[DIDServiceEndpoint]] = {
    for {
      _ <- serviceEndpoints.parse { list =>
        Either.cond(
          list.nonEmpty || !canBeEmpty,
          (),
          s"Service with id - $serviceId must have at least one service endpoint"
        )
      }
      validatedServiceEndpointsAndIndexes <- serviceEndpoints(identity).zipWithIndex
        .foldLeft(
          Either.right[ValidationError, List[(String, Int)]](List.empty)
        ) { (acc, uriAndIndex) =>
          val (uri, index) = uriAndIndex
          if (isValidUri(uri)) acc.map(list => (uri, index) :: list)
          else
            Left(
              InvalidValue(
                serviceEndpoints.path / index.toString,
                uri,
                s"Service endpoint - $uri of service with id - $serviceId is not a valid URI"
              )
            )
        }
        .map(_.reverse)
    } yield validatedServiceEndpointsAndIndexes.map { uriAndIndex =>
      val (uri, index) = uriAndIndex
      DIDServiceEndpoint(index, uri)
    }
  }

  def parseServiceType(serviceType: ValueAtPath[String]): Either[ValidationError, String] = Either.cond(
    serviceType(_.nonEmpty),
    serviceType(identity),
    MissingValue(serviceType.path)
  )

  def parseService(
      service: ValueAtPath[node_models.Service],
      didSuffix: DidSuffix
  ): Either[ValidationError, DIDService] = {

    for {
      id <- parseServiceId(service.child(_.id, "id"))
      serviceType <- parseServiceType(service.child(_.`type`, "type"))
      parsedServiceEndpoints <- parseServiceEndpoints(
        service.child(_.serviceEndpoint.toList, "serviceEndpoint"),
        id
      )
    } yield DIDService(
      id = id,
      `type` = serviceType,
      didSuffix = didSuffix,
      serviceEndpoints = parsedServiceEndpoints
    )

  }

  def parseKey(
      key: ValueAtPath[node_models.PublicKey],
      didSuffix: DidSuffix
  ): Either[ValidationError, DIDPublicKey] = {
    for {
      keyUsage <- key.child(_.usage, "usage").parse {
        case node_models.KeyUsage.MASTER_KEY => Right(KeyUsage.MasterKey)
        case node_models.KeyUsage.ISSUING_KEY => Right(KeyUsage.IssuingKey)
        case node_models.KeyUsage.COMMUNICATION_KEY =>
          Right(KeyUsage.CommunicationKey)
        case node_models.KeyUsage.REVOCATION_KEY =>
          Right(KeyUsage.RevocationKey)
        case node_models.KeyUsage.AUTHENTICATION_KEY =>
          Right(KeyUsage.AuthenticationKey)
        case _ => Left("Unknown value")
      }
      keyId <- parseKeyId(key.child(_.id, "id"))
      _ <- Either.cond(
        key(_.keyData.isDefined),
        (),
        MissingValue(key.path / "keyData")
      )
      publicKey <- parseKeyData(key)
    } yield DIDPublicKey(didSuffix, keyId, keyUsage, publicKey)
  }

  def parseHash(
      hash: ValueAtPath[ByteString]
  ): Either[ValidationError, Sha256Digest] = {
    hash.parse { hash =>
      Try(Sha256Digest.fromBytes(hash.toByteArray)).toEither.left
        .map(_.getMessage)
    }
  }

  def parseHashList(
      hashesV: ValueAtPath[Seq[ByteString]]
  ): Either[ValidationError, List[Sha256Digest]] = {
    hashesV.parse { hashes =>
      hashes.toList.traverse(h =>
        Try(Sha256Digest.fromBytes(h.toByteArray)).toEither.left
          .map(_.getMessage)
      )
    }
  }
}
