package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import io.iohk.node.errors
import io.iohk.node.models._
import io.iohk.node.models.DIDSuffix
import io.iohk.node.repositories.DIDDataRepository

class DIDDataService(didDataRepository: DIDDataRepository) {
  private val DID__RE = "(did:atala:)([0-9a-f]{64}$)".r
  def findByDID(did: String): FutureEither[errors.NodeError, DIDData] = {
    val DID__RE(_, didSuffix) = did
    didDataRepository.findByDidSuffix(DIDSuffix(didSuffix))
  }
}
