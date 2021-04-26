package io.iohk.atala.prism.app.grpc

data class ParticipantInfoResponse(
    val did: String,
    val name: String,
    val logo: ByteArray?,
    val token: String,
    val alreadyAdded: Boolean
)
