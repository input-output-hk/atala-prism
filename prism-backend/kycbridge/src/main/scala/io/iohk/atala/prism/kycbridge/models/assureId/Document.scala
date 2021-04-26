package io.iohk.atala.prism.kycbridge.models.assureId

case class Document(
    instanceId: String,
    biographic: Option[DocumentBiographic],
    classification: Option[DocumentClassification]
)

case class DocumentBiographic(
    age: Option[Int],
    birthDate: Option[String],
    expirationDate: Option[String],
    fullName: Option[String],
    gender: Option[Int],
    photo: Option[String]
)

case class DocumentClassification(
    `type`: Option[DocumentClassificationType]
)

case class DocumentClassificationType(
    `class`: Option[Int],
    className: Option[String],
    countryCode: Option[String],
    issue: Option[String], // issue year
    name: Option[String]
)
