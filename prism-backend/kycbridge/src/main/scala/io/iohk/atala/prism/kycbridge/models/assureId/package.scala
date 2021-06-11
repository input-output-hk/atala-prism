package io.iohk.atala.prism.kycbridge.models

import java.time.Instant

import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import java.time.ZoneOffset

package object assureId {

  object implicits {

    // assureId service only accepts json requests with capitalized key names.
    implicit val customConfig: Configuration =
      Configuration(_.capitalize, _.capitalize, useDefaults = true, discriminator = None)

    implicit val deviceTypeEncoder: Encoder[DeviceType] = deriveConfiguredEncoder
    implicit val deviceTypeDecoder: Decoder[DeviceType] = deriveConfiguredDecoder

    implicit val deviceEncoder: Encoder[Device] = deriveConfiguredEncoder
    implicit val deviceDecoder: Decoder[Device] = deriveConfiguredDecoder

    implicit val documentBiographicEncoder: Encoder[DocumentBiographic] = deriveConfiguredEncoder
    implicit val documentBiographicDecoder: Decoder[DocumentBiographic] = (c: HCursor) => {
      val knownFields = Set("Age", "BirthDate", "ExpirationDate", "FullName", "Gender", "Photo")
      for {
        age <- c.downField("Age").as[Option[Int]]
        birthDate <- parseDate(c.downField("BirthDate"))
        expirationDate <- parseDate(c.downField("ExpirationDate"))
        fullName <- c.downField("FullName").as[Option[String]]
        gender <- c.downField("Gender").as[Option[Int]]
        photo <- c.downField("Photo").as[Option[String]]
      } yield DocumentBiographic(
        age,
        birthDate,
        expirationDate,
        fullName,
        gender,
        photo,
        unknownFields = parseUnknownFields(c, knownFields)
      )
    }

    implicit val documentClassificationDetailsBackEncoder: Encoder[DocumentClassificationDetailsBack] =
      deriveConfiguredEncoder
    implicit val documentClassificationDetailsBackDecoder: Decoder[DocumentClassificationDetailsBack] =
      deriveConfiguredDecoder

    implicit val documentClassificationDetailsFrontEncoder: Encoder[DocumentClassificationDetailsFront] =
      deriveConfiguredEncoder
    implicit val documentClassificationDetailsFrontDecoder: Decoder[DocumentClassificationDetailsFront] =
      deriveConfiguredDecoder

    implicit val documentClassificationDetailsEncoder: Encoder[DocumentClassificationDetails] = deriveConfiguredEncoder
    implicit val documentClassificationDetailsDecoder: Decoder[DocumentClassificationDetails] = deriveConfiguredDecoder

    implicit val documentClassificationEncoder: Encoder[DocumentClassification] = deriveConfiguredEncoder
    implicit val documentClassificationDecoder: Decoder[DocumentClassification] = deriveConfiguredDecoder

    implicit val documentClassificationTypeEncoder: Encoder[DocumentClassificationType] = deriveConfiguredEncoder
    implicit val documentClassificationTypeDecoder: Decoder[DocumentClassificationType] = (c: HCursor) => {
      val knownFields = Set("Class", "ClassName", "CountryCode", "Issue", "Name")
      for {
        `class` <- c.downField("Class").as[Option[Int]]
        className <- c.downField("ClassName").as[Option[String]]
        countryCode <- c.downField("CountryCode").as[Option[String]]
        issue <- c.downField("Issue").as[Option[String]]
        name <- c.downField("Name").as[Option[String]]
      } yield DocumentClassificationType(
        `class`,
        className,
        countryCode,
        issue,
        name,
        unknownFields = parseUnknownFields(c, knownFields)
      )
    }

    implicit val documentDataFieldEncoder: Encoder[DocumentDataField] = deriveConfiguredEncoder
    implicit val documentDataFieldDecoder: Decoder[DocumentDataField] = deriveConfiguredDecoder

    implicit val documentEncoder: Encoder[Document] = deriveConfiguredEncoder
    implicit val documentDecoder: Decoder[Document] = deriveConfiguredDecoder

    implicit val newDocumentInstanceRequestBodyEncoder: Encoder[NewDocumentInstanceRequestBody] =
      deriveConfiguredEncoder
    implicit val newDocumentInstanceRequestBodyDecoder: Decoder[NewDocumentInstanceRequestBody] =
      deriveConfiguredDecoder

    private[models] def parseUnknownFields(c: HCursor, knownFields: Set[String]): List[String] = {
      c.downField("UnknownFields").as[List[String]] match {
        case Right(unknownFields) => unknownFields
        case Left(_) => c.keys.map(_.toList.filterNot(knownFields.contains)).getOrElse(Nil)
      }
    }

    private[models] def parseDate(date: ACursor): Either[DecodingFailure, Option[Instant]] = {
      date.as[Option[Instant]] match {
        case date: Right[DecodingFailure, Option[Instant]] => date
        case Left(_) => date.as[Option[String]].map(_.flatMap(parseDotNetDate))
      }
    }

    /**
      * Parse .net date in format: /Date(1325134800000)/
      */
    private[models] def parseDotNetDate(value: String): Option[Instant] = {
      val jsonDate = raw"\/Date\((-?\d+)(\+\d+)?\)\/".r

      value match {
        case jsonDate(milis, zone) =>
          Some(
            Instant
              .ofEpochMilli(milis.toLong)
              .atOffset(Option(zone).filter(_.nonEmpty).map(ZoneOffset.of).getOrElse(ZoneOffset.UTC))
              .toInstant
          )
        case _ => None
      }
    }

  }

}
