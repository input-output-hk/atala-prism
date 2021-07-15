package io.iohk.atala.prism.kycbridge.task.lease.system

import enumeratum.Enum
import io.iohk.atala.prism.task.lease.system.ProcessingTaskState

sealed abstract class KycBridgeProcessingTaskState(value: String) extends ProcessingTaskState(value)

object KycBridgeProcessingTaskState extends Enum[KycBridgeProcessingTaskState] {
  lazy val values = findValues

  final case object ProcessConnectorMessagesState
      extends KycBridgeProcessingTaskState("PROCESS_CONNECTOR_MESSAGES_STATE")

  final case object AcuantFetchDocumentDataState1
      extends KycBridgeProcessingTaskState("ACUANT_FETCH_DOCUMENT_DATA_STATE_1")
  final case object AcuantCompareImagesState2 extends KycBridgeProcessingTaskState("ACUANT_COMPARE_IMAGES_STATE_2")
  final case object AcuantIssueCredentialState3 extends KycBridgeProcessingTaskState("ACUANT_ISSUE_CREDENTIAL_STATE_3")

  final case object AcuantStartProcessForConnection
      extends KycBridgeProcessingTaskState("ACUANT_START_PROCESS_FOR_CONNECTION")
  final case object ProcessNewConnections extends KycBridgeProcessingTaskState("PROCESS_NEW_CONNECTIONS")

  final case object SendForAcuantManualReviewState extends KycBridgeProcessingTaskState("SEND_FOR_ACUANT_MANUAL_REVIEW")
  final case object SendForAcuantManualReviewPendingState
      extends KycBridgeProcessingTaskState("SEND_FOR_ACUANT_MANUAL_REVIEW_PENDING")
  final case object SendForAcuantManualReviewReadyState
      extends KycBridgeProcessingTaskState("SEND_FOR_ACUANT_MANUAL_REVIEW_READY")
}
