package io.iohk.atala.prism.kycbridge.models
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

package object acas {

  object implicits {
    //acas service only accepts json requests with snake_case key names.
    implicit val customConfig: Configuration =
      Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit val accessTokenRequestBodyEncoder: Encoder[AccessTokenRequestBody] = deriveConfiguredEncoder
    implicit val accessTokenRequestBodyDecoder: Decoder[AccessTokenRequestBody] = deriveConfiguredDecoder

    implicit val accessTokenResponseBodyEncoder: Encoder[AccessTokenResponseBody] = deriveConfiguredEncoder
    implicit val accessTokenResponseBodyDecoder: Decoder[AccessTokenResponseBody] = deriveConfiguredDecoder
  }

}
