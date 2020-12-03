package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.DIDDataRepository

class DIDDataService(didDataRepository: DIDDataRepository) {

  def findByDID(did: DID): FutureEither[errors.NodeError, DIDDataState] = {
    did.getCanonicalSuffix match {
      case Some(didSuffix) => didDataRepository.findByDidSuffix(didSuffix)
      case _ => Left(UnknownValueError("didSuffix", did.value)).toFutureEither
    }
  }
}
