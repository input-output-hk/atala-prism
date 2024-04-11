package io.iohk.atala.prism.node

import io.iohk.atala.prism.node.auth.{WhitelistedAuthHelper, model}
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.repositories.RequestNoncesRepository

class NodeExplorerAuthenticator(
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext]
) extends WhitelistedAuthHelper[IOWithTraceIdContext] {

  /** Burns given nonce for DID, so that the request can not be cloned by a malicious agent
    */
  override def burnNonce(did: PrismDid, requestNonce: model.RequestNonce): IOWithTraceIdContext[Unit] =
    requestNoncesRepository
      .burn(did, requestNonce)
}
