package io.iohk.atala.prism.kycbridge.models.identityMind

final case class ConsumerRequest(
    man: String,
    profile: String,
    scanData: String,
    backsideImageData: Option[String],
    faceImages: Seq[String],
    docType: Option[String],
    docCountry: Option[String]
)
