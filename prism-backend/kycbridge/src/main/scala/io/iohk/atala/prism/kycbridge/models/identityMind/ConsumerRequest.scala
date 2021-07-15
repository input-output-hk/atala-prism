package io.iohk.atala.prism.kycbridge.models.identityMind

final case class PostConsumerRequest(
    man: String,
    profile: String,
    scanData: String,
    backsideImageData: Option[String],
    faceImages: Seq[String],
    docType: Option[String],
    docCountry: Option[String]
)

final case class GetConsumerRequest(
    mtid: String
)
