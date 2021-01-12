package io.iohk.atala.prism.kycbridge.models
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

package object assureId {

  object implicits {

    //assureId service only accepts json requests with capitalized key names.
    implicit val customConfig: Configuration =
      Configuration(_.capitalize, _.capitalize, useDefaults = true, discriminator = None)

    implicit val deviceTypeEncoder: Encoder[DeviceType] = deriveConfiguredEncoder
    implicit val deviceTypeDecoder: Decoder[DeviceType] = deriveConfiguredDecoder

    implicit val deviceEncoder: Encoder[Device] = deriveConfiguredEncoder
    implicit val deviceDecoder: Decoder[Device] = deriveConfiguredDecoder

    implicit val documentBiographicEncoder: Encoder[DocumentBiographic] = deriveConfiguredEncoder
    implicit val documentBiographicDecoder: Decoder[DocumentBiographic] = deriveConfiguredDecoder

    implicit val documentClassificationEncoder: Encoder[DocumentClassification] = deriveConfiguredEncoder
    implicit val documentClassificationDecoder: Decoder[DocumentClassification] = deriveConfiguredDecoder

    implicit val documentClassificationTypeEncoder: Encoder[DocumentClassificationType] = deriveConfiguredEncoder
    implicit val documentClassificationTypeDecoder: Decoder[DocumentClassificationType] = deriveConfiguredDecoder

    implicit val documentEncoder: Encoder[Document] = deriveConfiguredEncoder
    implicit val documentDecoder: Decoder[Document] = deriveConfiguredDecoder

    implicit val newDocumentInstanceRequestBodyEncoder: Encoder[NewDocumentInstanceRequestBody] =
      deriveConfiguredEncoder
    implicit val newDocumentInstanceRequestBodyDecoder: Decoder[NewDocumentInstanceRequestBody] =
      deriveConfiguredDecoder

  }

}
