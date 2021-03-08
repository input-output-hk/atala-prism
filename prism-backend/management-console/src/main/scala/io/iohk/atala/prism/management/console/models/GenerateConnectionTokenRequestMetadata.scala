package io.iohk.atala.prism.management.console.models

case class GenerateConnectionTokenRequestMetadata(
    did: String,
    didKeyId: String,
    didSignature: String,
    requestNonce: String
)
