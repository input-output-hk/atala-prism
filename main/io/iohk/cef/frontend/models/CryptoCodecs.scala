package io.iohk.cef.frontend.models

import io.iohk.crypto._
import play.api.libs.json.{JsObject, Writes}

trait CryptoCodecs {

  implicit def signingKeyPairWrites(
      implicit pub: Writes[SigningPublicKey],
      priv: Writes[SigningPrivateKey]
  ): Writes[SigningKeyPair] = Writes { obj =>
    val map = Map(
      "publicKey" -> pub.writes(obj.public),
      "privateKey" -> priv.writes(obj.`private`)
    )

    JsObject(map)
  }
}
