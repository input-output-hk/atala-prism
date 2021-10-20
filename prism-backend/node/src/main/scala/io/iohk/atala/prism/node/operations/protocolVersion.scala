package io.iohk.atala.prism.node.operations

import io.iohk.atala.prism.node.models.ProtocolVersion
import io.iohk.atala.prism.node.models.ProtocolVersion.ProtocolVersion1_0

/** This package object contains all hardcoded information about protocol versions.
  */
package object protocolVersion {

  trait SupportedOperations {
    def isOperationSupportedInVersion(operation: Operation, protocol: ProtocolVersion): Boolean
  }

  implicit object SupportedOperationsInst extends SupportedOperations {
    override def isOperationSupportedInVersion(operation: Operation, protocolV: ProtocolVersion): Boolean =
      (protocolV, operation) match {
        case (
              ProtocolVersion1_0,
              _: CreateDIDOperation | _: UpdateDIDOperation | _: IssueCredentialBatchOperation |
              _: RevokeCredentialsOperation | _: ProtocolVersionUpdateOperation
            ) =>
          true
        case _ => false
      }
  }
}
