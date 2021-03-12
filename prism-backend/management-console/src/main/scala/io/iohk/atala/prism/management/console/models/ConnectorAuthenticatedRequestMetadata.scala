package io.iohk.atala.prism.management.console.models

case class ConnectorAuthenticatedRequestMetadata(
    did: String,
    didKeyId: String,
    didSignature: String,
    requestNonce: String
)
