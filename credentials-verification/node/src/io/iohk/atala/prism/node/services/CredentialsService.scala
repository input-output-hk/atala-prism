package io.iohk.atala.prism.node.services

import io.iohk.cvp.utils.FutureEither
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.repositories.CredentialsRepository

class CredentialsService(credentialsRepository: CredentialsRepository) {
  def getCredentialState(credentialId: CredentialId): FutureEither[errors.NodeError, CredentialState] = {
    credentialsRepository
      .find(credentialId)
  }
}
