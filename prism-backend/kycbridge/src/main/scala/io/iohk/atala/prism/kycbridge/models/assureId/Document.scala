package io.iohk.atala.prism.kycbridge.models.assureId

import java.time.Instant

case class Document(
    instanceId: String,
    biographic: Option[DocumentBiographic],
    classification: Option[DocumentClassification],
    dataFields: Option[List[DocumentDataField]]
) {

  def getDataField(name: String): Option[DocumentDataField] =
    dataFields.flatMap(_.find(_.name.contains(name)))
}

case class DocumentBiographic(
    age: Option[Int],
    birthDate: Option[Instant],
    expirationDate: Option[Instant],
    fullName: Option[String],
    gender: Option[Int],
    photo: Option[String],
    unknownFields: List[String] // list of fields that are present in JSON but not held in Circe decoder
)

case class DocumentClassification(
    `type`: Option[DocumentClassificationType],
    classificationDetails: Option[DocumentClassificationDetails]
)

case class DocumentClassificationDetails(
    back: Option[DocumentClassificationDetailsBack],
    front: Option[DocumentClassificationDetailsFront]
)

case class DocumentClassificationDetailsBack(
    issue: Option[String],
    issuerType: Option[Int],
    issuerCode: Option[String],
    issuerName: Option[String],
    countryCode: Option[String]
)

case class DocumentClassificationDetailsFront(
    name: Option[String]
)

case class DocumentClassificationType(
    `class`: Option[Int],
    className: Option[String],
    countryCode: Option[String],
    issue: Option[String], // issue year
    name: Option[String],
    unknownFields: List[String] // list of fields that are present in JSON but not held in Circe decoder
)

case class DocumentDataField(
    key: Option[String],
    name: Option[String],
    value: Option[String]
)
