package io.iohk.cvp.grpc

import io.iohk.atala.prism.protos.ParticipantInfo

data class ParticipantInfoResponse(
        val participantInfo: ParticipantInfo,
        val token: String,
        val alreadyAdded: Boolean
)