package io.iohk.atala.prism.node.services

import io.iohk.atala.identity.DID
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.DIDDataRepository

class DIDDataService(didDataRepository: DIDDataRepository) {

  def findByDID(did: String): FutureEither[errors.NodeError, DIDDataState] = {
    DID.getCanonicalSuffix(did) match {
      case Some(didSuffix) => didDataRepository.findByDidSuffix(DIDSuffix(didSuffix))
      case _ => Left(UnknownValueError("didSuffix", did)).toFutureEither
    }
  }
}
