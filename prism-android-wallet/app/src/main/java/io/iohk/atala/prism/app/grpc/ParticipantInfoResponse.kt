package io.iohk.atala.prism.app.grpc

import io.iohk.atala.prism.protos.ParticipantInfo

data class ParticipantInfoResponse(
    val participantInfo: ParticipantInfo,
    val token: String,
    val alreadyAdded: Boolean
)
