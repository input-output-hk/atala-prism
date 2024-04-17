package io.iohk.atala.prism.node.operations

import cats.implicits._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.node.models.{PublicKeyData, DIDPublicKey, DIDService, DidSuffix, KeyUsage, ProtocolConstants}
import io.iohk.atala.prism.node.operations.ValidationError.{InvalidValue, MissingValue}
import io.iohk.atala.prism.node.operations.path.ValueAtPath
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.node.utils.UriUtils
import io.circe.parser.{parse => parseJson}
import io.circe.Json
import io.iohk.atala.prism.node.crypto.CryptoUtils

import scala.util.Try

object ParsingUtils {

  private type EitherValidationError[B] = Either[ValidationError, B]

  def parseKeyData(
      keyData: ValueAtPath[node_models.PublicKey]
  ): Either[ValidationError, PublicKeyData] = {
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

  // We only accept Secp256k1 keys in uncompressed format
  def parseECKey(
      ecData: ValueAtPath[node_models.ECKeyData]
  ): Either[ValidationError, PublicKeyData] = {

    val curve = ecData(_.curve)

    if (ecData(_.curve) != ProtocolConstants.secpCurveName) {
      Left(ecData.child(_.curve, "curve").invalid(s"Unsupported curve - $curve"))
    } else if (ecData(_.x.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "x").missing())
    } else if (ecData(_.y.toByteArray.isEmpty)) {
      Left(ecData.child(_.curve, "y").missing())
    } else {
      Try {
        val key = CryptoUtils.unsafeToSecpPublicKeyFromByteCoordinates(
          ecData(_.x.toByteArray),
          ecData(_.y.toByteArray)
        )
        PublicKeyData(key.curveName, key.compressed.toVector)
      }.toEither.left
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
  ): Either[ValidationError, PublicKeyData] = {

    val supportedCurves = ProtocolConstants.supportedEllipticCurves
    val curve = ecData(_.curve)

    if (ecData(_.data.toByteArray.isEmpty)) {
      Left(ecData.child(_.data, "compressedData").missing())
    } else if (!supportedCurves.contains(curve)) {
      Left(ecData.child(_.curve, "curve").invalid(s"Unsupported curve - $curve"))
    } else {
      Right(
        PublicKeyData(curve, ecData(_.data.toByteArray).toVector)
      )
    }
  }

  def parseKeyId(keyId: ValueAtPath[String], idCharLenLimit: Int): Either[ValidationError, String] = {
    val keyIdValue = keyId(identity)
    for {
      idAsValidUriFragment <- Either.cond(
        UriUtils.isValidUriFragment(keyIdValue),
        keyIdValue,
        keyId.invalid(s"Public key id: \"$keyIdValue\" is not a valid URI fragment")
      )
      idWithValidLength <- Either.cond(
        idAsValidUriFragment.length <= idCharLenLimit,
        idAsValidUriFragment,
        keyId.invalid(
          s"Exceeded public key id character limit for a key with id - $keyIdValue, max - $idCharLenLimit, got - ${idAsValidUriFragment.length}"
        )
      )
    } yield idWithValidLength
  }

  def parseServiceId(
      serviceId: ValueAtPath[String],
      idCharLenLimit: Int
  ): Either[ValidationError, String] = {
    val serviceIdValue = serviceId(identity)
    for {
      idAsValidUriFragment <- Either.cond(
        UriUtils.isValidUriFragment(serviceIdValue),
        serviceIdValue,
        serviceId.invalid(s"Service id: \"$serviceIdValue\" is not a valid URI fragment")
      )
      idWithValidLength <- Either.cond(
        idAsValidUriFragment.length <= idCharLenLimit,
        idAsValidUriFragment,
        serviceId.invalid(
          s"Exceeded service id character limit for a service with id - $serviceIdValue, max - $idCharLenLimit, got - ${idAsValidUriFragment.length}"
        )
      )
    } yield idWithValidLength

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
          str.length <= serviceEndpointCharLimit,
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
          else {
            if (UriUtils.isValidUriString(rawServiceEndpoints)) rawServiceEndpoints.asRight
            else
              serviceEndpoints
                .invalid(
                  s"Service endpoint - $rawServiceEndpoints of service with id - $serviceId is not a valid URI"
                )
                .asLeft
          }
        case Right(json) =>
          // is a JSON string, but must be either array or object
          json.asArray match {
            case Some(endpoints) =>
              // is an array, but can only be array of strings or objects or mixed of those
              // Iterate over every element, if string and valid URI, normalize it, if invalid URI, fail the whole thing
              // if not a string, then must be an object, no validations on objects
              // if neither string or an object, fail the whole thing.

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
                    else {
                      val strValidated = jsonVal.asString
                        .map(str => (str, UriUtils.isValidUriString(str)))
                        .map { case (str, isValidUri) =>
                          if (isValidUri) Json.fromString(str).asRight
                          else
                            InvalidValue(
                              serviceEndpoints.path / index.toString,
                              rawServiceEndpoints,
                              s"Service endpoint - ${jsonVal.noSpaces} inside $rawServiceEndpoints of service with id - $serviceId is not a valid URI"
                            ).asLeft
                        }
                        .toRight(
                          InvalidValue(
                            serviceEndpoints.path / index.toString,
                            rawServiceEndpoints,
                            s"Service endpoints of service with id - $serviceId must be an array of either valid URI strings or objects "
                          )
                        )
                        .flatten
                      strValidated
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
            str.length <= serviceTypeCharLimit,
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
      serviceTypeCharLimit: Int,
      idCharLenLimit: Int
  ): Either[ValidationError, DIDService] = {

    for {
      id <- parseServiceId(service.child(_.id, "id"), idCharLenLimit)
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

  def parseContext(
      context: ValueAtPath[List[String]],
      contextStringCharLimit: Int
  ): Either[ValidationError, List[String]] = {

    for { // InvalidValue(path, value.toString, message)
      // Validate each context string is withing char limit and is a valid URI
      contextStringsUriAndLimitValidated <- context { contextStrs =>
        contextStrs.zipWithIndex.traverse[EitherValidationError, String] { case (str, index) =>
          if (!UriUtils.isValidUriString(str))
            InvalidValue(context.path / index.toString, str, s"\"$str\" is not a valid URI").asLeft[String]
          else if (str.length > contextStringCharLimit)
            InvalidValue(
              context.path / index.toString,
              str,
              s"Exceeded type character limit for a context string, max - $contextStringCharLimit, got - ${str.length}"
            ).asLeft[String]
          else str.asRight[ValidationError]
        }
      }
      // validate context includes only unique context strings
      uniqueValidated <- Either.cond(
        contextStringsUriAndLimitValidated.distinct.size == contextStringsUriAndLimitValidated.size,
        contextStringsUriAndLimitValidated,
        context.invalid("List of context strings contains duplicates")
      )

    } yield uniqueValidated
  }

  def parseKey(
      key: ValueAtPath[node_models.PublicKey],
      didSuffix: DidSuffix,
      idCharLenLimit: Int
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
      keyId <- parseKeyId(key.child(_.id, "id"), idCharLenLimit)
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
