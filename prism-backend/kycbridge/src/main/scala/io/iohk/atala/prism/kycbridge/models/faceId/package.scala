package io.iohk.atala.prism.kycbridge.models

import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

package object faceId {

  object implicits {

    //assureId service only accepts json requests with capitalized key names.
    implicit val customConfig: Configuration =
      Configuration(_.capitalize, _.capitalize, useDefaults = true, discriminator = None)

    implicit val dataEncoder: Encoder[Data] = deriveConfiguredEncoder
    implicit val dataDecoder: Decoder[Data] = deriveConfiguredDecoder

    implicit val settingsEncoder: Encoder[Settings] = deriveConfiguredEncoder
    implicit val settingsDecoder: Decoder[Settings] = deriveConfiguredDecoder

    implicit val faceMatchRequestEncoder: Encoder[FaceMatchRequest] = deriveConfiguredEncoder
    implicit val faceMatchRequestDecoder: Decoder[FaceMatchRequest] = deriveConfiguredDecoder

    implicit val faceMatchResponseEncoder: Encoder[FaceMatchResponse] = deriveConfiguredEncoder
    implicit val faceMatchResponseDecoder: Decoder[FaceMatchResponse] = deriveConfiguredDecoder

  }
}
