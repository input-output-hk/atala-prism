package io.iohk.atala.prism.node.repositories.daos

import doobie.implicits._
import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.node.identity.{PrismDid => DID}

object RequestNoncesDAO {

  def burn(did: DID, requestNonce: RequestNonce): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO did_request_nonces (request_nonce, did)
         |VALUES ($requestNonce, $did)
         |ON CONFLICT DO NOTHING
         |RETURNING request_nonce
         |""".stripMargin.query[RequestNonce].option.map {
      case Some(_) => ()
      case None => throw new RuntimeException("The nonce was already used")
    }
  }
}
