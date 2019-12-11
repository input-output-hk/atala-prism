package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import io.iohk.node.errors
import io.iohk.node.models._
import io.iohk.node.models.DIDSuffix
import io.iohk.node.repositories.DIDDataRepository

class DIDDataService(didDataRepository: DIDDataRepository){
  def findByDIDSuffix(didSuffix: DIDSuffix): FutureEither[errors.NodeError, DIDData] = {
      didDataRepository.findByDidSuffix(didSuffix)
  }
}
