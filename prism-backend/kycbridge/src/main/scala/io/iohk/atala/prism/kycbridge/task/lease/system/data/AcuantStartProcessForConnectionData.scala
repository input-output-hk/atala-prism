package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._

case class AcuantStartProcessForConnectionData(
    receivedMessageId: String,
    connectionId: String
)

object AcuantStartProcessForConnectionData {
  implicit val acuantStartProcessForConnectionDataEncoder: Encoder[AcuantStartProcessForConnectionData] = deriveEncoder
  implicit val acuantStartProcessForConnectionDataDecoder: Decoder[AcuantStartProcessForConnectionData] = deriveDecoder
}
