package io.iohk.atala.prism.kycbridge.models.assureId

case class Document(
    instanceId: String,
    biographic: Option[DocumentBiographic],
    classification: Option[DocumentClassification]
)

case class DocumentBiographic(
    age: Int,
    birthDate: String,
    expirationDate: String,
    fullName: String,
    gender: Int,
    photo: String
)

case class DocumentClassification(
    `type`: DocumentClassificationType
)

case class DocumentClassificationType(
    `class`: Int,
    className: String,
    countryCode: String,
    issue: String, // issue year
    name: String
)
