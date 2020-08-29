package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.DIDDataRepository

class DIDDataService(didDataRepository: DIDDataRepository) {

  private val DID__RE = "(did:prism:)([0-9a-f]{64}$)".r

  def findByDID(did: String): FutureEither[errors.NodeError, DIDDataState] = {
    val mayBeDidSuffix = DID__RE.findFirstIn(did).map { matched =>
      val DID__RE(_, didSuffix) = matched
      didSuffix
    }
    mayBeDidSuffix match {
      case Some(didSuffix) => didDataRepository.findByDidSuffix(DIDSuffix(didSuffix))
      case _ => Left(UnknownValueError("didSuffix", did)).toFutureEither
    }
  }
}
