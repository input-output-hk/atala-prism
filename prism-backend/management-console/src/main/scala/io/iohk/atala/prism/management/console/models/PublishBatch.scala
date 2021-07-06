package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class PublishBatch(
    signedOperation: SignedAtalaOperation
)
