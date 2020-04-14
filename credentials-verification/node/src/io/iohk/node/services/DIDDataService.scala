package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.node.errors
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models._
import io.iohk.node.models.DIDSuffix
import io.iohk.node.repositories.DIDDataRepository
import scala.concurrent.ExecutionContext

class DIDDataService(didDataRepository: DIDDataRepository)(implicit ec: ExecutionContext) {

  private val DID__RE = "(did:prism:)([0-9a-f]{64}$)".r

  def findByDID(did: String): FutureEither[errors.NodeError, DIDData] = {
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
