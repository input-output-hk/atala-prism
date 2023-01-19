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
import io.iohk.atala.prism.utils.UriUtils

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

  def parseKeyId(keyId: ValueAtPath[String]): Either[ValidationError, String] = {
    keyId.parse { id =>
      Either.cond(
        UriUtils.isValidUriFragment(id),
        id,
        s"Key id: \"$id\" is not a valid URI fragment"
      )
    }
  }

  def parseServiceId(
      serviceId: ValueAtPath[String]
  ): Either[ValidationError, String] =
    serviceId.parse { id =>
      Either.cond(
        UriUtils.isValidUriFragment(id),
        id,
        s"Service id: \"$id\" is not a valid URI fragment"
      )
    }

  def parseServiceEndpoints(
      serviceEndpoints: ValueAtPath[List[String]],
      serviceId: String,
      canBeEmpty: Boolean = false
  ): Either[ValidationError, List[DIDServiceEndpoint]] = {
    type EitherValidationError[B] = Either[ValidationError, B]
    for {
      _ <- serviceEndpoints.parse { list =>
        Either.cond(
          list.nonEmpty || canBeEmpty,
          (),
          s"Service with id - $serviceId must have at least one service endpoint"
        )
      }
      validatedServiceEndpointsAndIndexes <- serviceEndpoints(identity).zipWithIndex
        .traverse[EitherValidationError, (String, Int)] { case (uri, index) =>
          UriUtils
            .normalizeUri(uri)
            .map(uri => (uri, index))
            .toRight(
              InvalidValue(
                serviceEndpoints.path / index.toString,
                uri,
                s"Service endpoint - $uri of service with id - $serviceId is not a valid URI"
              )
            )

        }
    } yield validatedServiceEndpointsAndIndexes.map { case (uri, index) =>
      DIDServiceEndpoint(index, uri)
    }
  }

  def parseServiceType(serviceType: ValueAtPath[String], canBeEmpty: Boolean = false): Either[ValidationError, String] =
    Either.cond(
      serviceType(tp => tp.trim.nonEmpty || canBeEmpty),
      serviceType(_.trim),
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
        case node_models.KeyUsage.KEY_AGREEMENT_KEY =>
          Right(KeyUsage.KeyAgreementKey)
        case node_models.KeyUsage.REVOCATION_KEY =>
          Right(KeyUsage.RevocationKey)
        case node_models.KeyUsage.AUTHENTICATION_KEY =>
          Right(KeyUsage.AuthenticationKey)
        case node_models.KeyUsage.CAPABILITY_INVOCATION_KEY =>
          Right(KeyUsage.CapabilityInvocationKey)
        case node_models.KeyUsage.CAPABILITY_DELEGATION_KEY =>
          Right(KeyUsage.CapabilityDelegationKey)
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
