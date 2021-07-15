package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper

case class SendForAcuantManualReviewPendingStateData(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Base64ByteArrayWrapper,
    mtid: String
)

object SendForAcuantManualReviewPendingStateData {
  implicit val sendForAcuantManualReviewStatePendingDataEncoder: Encoder[SendForAcuantManualReviewPendingStateData] =
    deriveEncoder
  implicit val sendForAcuantManualReviewStatePendingDataDecoder: Decoder[SendForAcuantManualReviewPendingStateData] =
    deriveDecoder
}
