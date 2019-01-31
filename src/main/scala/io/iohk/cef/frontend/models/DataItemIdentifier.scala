package io.iohk.cef.frontend.models

import io.iohk.crypto.Signature
import io.iohk.cef.data.DataItemId
import play.api.libs.json.{Format, Json}

case class DataItemIdentifier(id: DataItemId, signature: Signature)

object DataItemIdentifier {

  implicit val DataItemIdentifierJsonFormat: Format[DataItemIdentifier] =
    Json.format[DataItemIdentifier]

}
