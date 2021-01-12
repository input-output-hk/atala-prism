package io.iohk.atala.cvp.webextension

import io.circe.{Decoder, Encoder}
import io.iohk.atala.prism.identity.DID

package object circe {
  implicit val didEncoder = Encoder[String].contramap[DID](_.value)
  implicit val didDecoder = Decoder[String].map[DID](s => DID.unsafeFromString(s))
}
