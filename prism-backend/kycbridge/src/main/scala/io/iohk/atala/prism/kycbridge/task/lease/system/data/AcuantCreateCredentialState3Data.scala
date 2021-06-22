package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper._

case class AcuantCreateCredentialState3Data(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Base64ByteArrayWrapper,
    document: Document,
    frontScannedImage: Base64ByteArrayWrapper
)

object AcuantCreateCredentialState3Data {
  def fromAcuantCompareImagesState2Data(
      data: AcuantCompareImagesState2Data,
      frontScannedImage: Array[Byte]
  ): AcuantCreateCredentialState3Data =
    AcuantCreateCredentialState3Data(
      receivedMessageId = data.receivedMessageId,
      connectionId = data.connectionId,
      documentInstanceId = data.documentInstanceId,
      selfieImage = data.selfieImage,
      document = data.document,
      frontScannedImage = Base64ByteArrayWrapper(frontScannedImage)
    )

  implicit val acuantProcessingDataEncoder: Encoder[AcuantCreateCredentialState3Data] = deriveEncoder
  implicit val acuantProcessingDataDecoder: Decoder[AcuantCreateCredentialState3Data] = deriveDecoder
}
