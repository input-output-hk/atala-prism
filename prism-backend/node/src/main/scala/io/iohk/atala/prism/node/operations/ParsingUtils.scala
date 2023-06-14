package io.iohk.atala.prism.node.operations

import java.time.LocalDate
import cats.implicits._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDService, KeyUsage}
import io.iohk.atala.prism.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.atala.prism.node.operations.path.ValueAtPath
import io.iohk.atala.prism.protos.{common_models, node_models}
import io.iohk.atala.prism.utils.UriUtils
import io.circe.parser.{parse => parseJson}
import io.circe.Json

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
    } else if (ecData(_.curve) != ECConfig.getCURVE_NAME) {
      Left(ecData.child(_.curve, "curve").invalid("Unsupported curve"))
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
      serviceEndpoints: ValueAtPath[String],
      serviceId: String,
      serviceEndpointCharLimit: Int,
      canBeEmpty: Boolean = false
  ): Either[ValidationError, String] = {
    for {
      // check for an empty string
      _ <- serviceEndpoints.parse { str =>
        Either.cond(
          str.nonEmpty || canBeEmpty,
          (),
          s"Service with id - $serviceId must have at least one service endpoint"
        )
      }
      // check for service endpoint char limit
      // NOTE: this check does account for spaces as well in case service endpoint is a JSON with spaces.
      _ <- serviceEndpoints.parse { str =>
        Either.cond(
          str.length < serviceEndpointCharLimit,
          (),
          s"Exceeded service endpoint character limit for a service with id - $serviceId, max - $serviceEndpointCharLimit, got - ${str.length}"
        )
      }
      rawServiceEndpoints = serviceEndpoints(identity)

      /*
       * Service endpoints string can be either:
       *  JSON string, must be either:
       *    JSON array, every element in the array can be either:
       *      regular string
       *        must be a valid URI
       *      JSON object
       *    JSON object
       *  regular string
       *    must be a valid URI
       */
      parsedServiceEndpoints <- parseJson(rawServiceEndpoints) match {
        case Left(_) =>
          // Not a JSON string, but could be a regular Uri string, if so, normalize it
          // First, make sure it is not empty, when canBeEmpty is true
          if (rawServiceEndpoints.isEmpty && canBeEmpty) rawServiceEndpoints.asRight
          else
            UriUtils
              // This function returns None for empty strings
              .normalizeUri(rawServiceEndpoints)
              .toRight(
                serviceEndpoints.invalid(
                  s"Service endpoint - $rawServiceEndpoints of service with id - $serviceId is not a valid URI"
                )
              )
        case Right(json) =>
          // is a JSON string, but must be either array or object
          json.asArray match {
            case Some(endpoints) =>
              // is an array, but can only be array of strings or objects or mixed of those
              // Iterate over every element, if string and valid URI, normalize it, if invalid URI, fail the whole thing
              // if not a string, then must be an object, no validations on objects
              // if neither string or an object, fail the whole thing.

              type EitherValidationError[B] = Either[ValidationError, B]
              // first check for empty array
              for {
                _ <- Either.cond(
                  endpoints.nonEmpty, // empty JSON array is invalid
                  (),
                  serviceEndpoints.invalid(s"Service with id - $serviceId invalid service endpoints - empty JSON array")
                )
                jsonArrValidated <- endpoints.zipWithIndex
                  .traverse[EitherValidationError, Json] { case (jsonVal, index) =>
                    if (jsonVal.isObject) jsonVal.asRight // We don't validate objets because there is no expected shape
                    else if (jsonVal.isString) {
                      // normalize a string, if succeed, append to final list, if not, fail the whole thing
                      val strNormalized = UriUtils
                        .normalizeUri(jsonVal.asString.get) // Should not fail because of .isString check above
                        .map(Json.fromString)
                        .toRight(
                          InvalidValue(
                            serviceEndpoints.path / index.toString,
                            rawServiceEndpoints,
                            s"Service endpoint - ${jsonVal.toString} inside $rawServiceEndpoints of service with id - $serviceId is not a valid URI"
                          )
                        )
                      strNormalized
                    } else {
                      // If Json is neither an object nor string, fail the whole thing
                      InvalidValue(
                        serviceEndpoints.path / index.toString,
                        rawServiceEndpoints,
                        s"Service endpoints of service with id - $serviceId must be an array of either valid URI strings or objects "
                      ).asLeft
                    }
                  }
                  .map(Json.arr(_: _*).noSpaces)
              } yield jsonArrValidated

            case None =>
              // is not an array, but could be an object
              if (json.isObject) json.noSpaces.asRight
              else
                serviceEndpoints
                  .invalid(
                    s"Service endpoint - $rawServiceEndpoints of service with id - $serviceId must be a JSON object or an array"
                  )
                  .asLeft
          }
      }
    } yield parsedServiceEndpoints
  }

  def parseServiceType(
      serviceType: ValueAtPath[String],
      canBeEmpty: Boolean = false,
      serviceTypeCharLimit: Int
  ): Either[ValidationError, String] = {

    def isValidTypeString(str: String): Boolean = {
      val pattern = """^[A-Za-z0-9\-_]+(\s*[A-Za-z0-9\-_])*$""".r
      str.trim.nonEmpty && str.trim.length == str.length && pattern.findFirstMatchIn(str).isDefined
    }

    /*
     * type string can be either
     *   regular string
     *     MUST not start nor end with whitespaces, and MUST have at least a non whitespace character
     *   JSON string
     *     can be only a JSON array, where each element is a string, every string
     *       MUST not start nor end with whitespaces, and MUST have at least a non whitespace character
     */

    /** I can start with an if statement, if it is empty and can be empty, i just return Right(string) otherwise a fire
      * a validation logic below
      */
    val rawType = serviceType(identity)
    if (rawType.isEmpty && canBeEmpty) rawType.asRight[ValidationError]
    else
      for {
        _ <- Either.cond(
          serviceType(tp => tp.trim.nonEmpty),
          (),
          serviceType.missing()
        )
        _ <- serviceType.parse { str =>
          Either.cond(
            str.length < serviceTypeCharLimit,
            (),
            s"Exceeded type character limit for a service, max - $serviceTypeCharLimit, got - ${str.length}"
          )
        }
        parsedType <- parseJson(rawType) match {
          case Left(_) =>
            // Not a JSON string, validate that it has at least one non whitespace character and no whitespaces around
            if (isValidTypeString(rawType)) rawType.asRight
            else
              serviceType
                .invalid(
                  "Invalid type string"
                )
                .asLeft
          case Right(jsonValue) =>
            // Is a JSON, validate that it is an array of valid strings

            if (jsonValue.isString || jsonValue.isObject)
              serviceType
                .invalid(
                  s"type must be a JSON array or regular string"
                )
                .asLeft
            else if (jsonValue.isArray) {
              val types = jsonValue.asArray.get
              // Is an array, validate an array, empty array of json is invalid
              for {
                _ <- Either.cond(
                  types.nonEmpty,
                  (),
                  serviceType.invalid("Type must be a non empty JSON array")
                )
                // If at least one element in an array is invalid, the whole thing is invalid
                validated <- types.zipWithIndex
                  // Find invalid one
                  .find { case (elm, _) =>
                    val validStr = elm.isString && {
                      val str = elm.asString.get // Will not fail because of check above
                      val vld = isValidTypeString(str)
                      vld
                    }
                    val validNum = elm.isNumber
                    !(validStr || validNum) // Must be either valid string or number
                  }
                  .map { case (_, index) =>
                    InvalidValue(
                      serviceType.path / index.toString,
                      rawType,
                      s"Invalid type string"
                    )
                  }
                  // Get the original raw type string as "right"
                  .toLeft(rawType)
              } yield validated
            } else if (jsonValue.isNumber || jsonValue.isNull || jsonValue.isBoolean) jsonValue.noSpaces.asRight
            else
              serviceType
                .invalid(
                  "Invalid type string"
                )
                .asLeft

        }
      } yield parsedType

  }

  def parseService(
      service: ValueAtPath[node_models.Service],
      didSuffix: DidSuffix,
      serviceEndpointCharLimit: Int,
      serviceTypeCharLimit: Int
  ): Either[ValidationError, DIDService] = {

    for {
      id <- parseServiceId(service.child(_.id, "id"))
      serviceType <- parseServiceType(
        serviceType = service.child(_.`type`, "type"),
        serviceTypeCharLimit = serviceTypeCharLimit
      )
      parsedServiceEndpoints <- parseServiceEndpoints(
        service.child(_.serviceEndpoint, "serviceEndpoint"),
        id,
        serviceEndpointCharLimit
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
