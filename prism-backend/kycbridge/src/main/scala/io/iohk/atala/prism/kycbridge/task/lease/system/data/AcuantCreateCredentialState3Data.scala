package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._

case class AcuantCreateCredentialState3Data(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Array[Byte],
    document: Document,
    frontScannedImage: Array[Byte]
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
      frontScannedImage = frontScannedImage
    )

  implicit val acuantProcessingDataEncoder: Encoder[AcuantCreateCredentialState3Data] = deriveEncoder
  implicit val acuantProcessingDataDecoder: Decoder[AcuantCreateCredentialState3Data] = deriveDecoder
}
