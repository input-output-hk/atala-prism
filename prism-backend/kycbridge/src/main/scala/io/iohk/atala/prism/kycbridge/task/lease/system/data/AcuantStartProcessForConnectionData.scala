package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.models.ConnectionToken

case class AcuantStartProcessForConnectionData(connectionToken: ConnectionToken)

object AcuantStartProcessForConnectionData {
  implicit val acuantStartProcessForConnectionDataEncoder: Encoder[AcuantStartProcessForConnectionData] = deriveEncoder
  implicit val acuantStartProcessForConnectionDataDecoder: Decoder[AcuantStartProcessForConnectionData] = deriveDecoder
}
