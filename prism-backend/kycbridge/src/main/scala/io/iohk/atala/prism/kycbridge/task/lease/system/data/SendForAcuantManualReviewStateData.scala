package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper._

case class SendForAcuantManualReviewStateData(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Base64ByteArrayWrapper
)

object SendForAcuantManualReviewStateData {
  implicit val sendForAcuantManualReviewStateDataEncoder: Encoder[SendForAcuantManualReviewStateData] = deriveEncoder
  implicit val sendForAcuantManualReviewStateDataDecoder: Decoder[SendForAcuantManualReviewStateData] = deriveDecoder
}
