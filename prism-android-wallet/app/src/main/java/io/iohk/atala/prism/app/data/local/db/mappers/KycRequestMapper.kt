package io.iohk.atala.prism.app.data.local.db.mappers

import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import io.iohk.atala.prism.protos.KycBridgeMessage

class KycRequestMapper {
    companion object {
        fun map(
            connectionId: String,
            messageId: String,
            message: KycBridgeMessage
        ): KycRequest? {
            return when (message.messageCase) {
                KycBridgeMessage.MessageCase.START_ACUANT_PROCESS -> {
                    val bearerToken = message.startAcuantProcess.bearerToken
                    val instanceId = message.startAcuantProcess.documentInstanceId
                    KycRequest(connectionId, messageId, bearerToken, instanceId)
                }
                else -> null
            }
        }
    }
}
