package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import io.iohk.node.errors
import io.iohk.node.models.CredentialId
import io.iohk.node.models.nodeState.CredentialState
import io.iohk.node.repositories.CredentialsRepository

class CredentialsService(credentialsRepository: CredentialsRepository) {
  def getCredentialState(credentialId: CredentialId): FutureEither[errors.NodeError, CredentialState] = {
    credentialsRepository
      .find(credentialId)
  }
}
