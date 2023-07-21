package io.iohk.atala.prism.vault.repositories.daos

import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.identity.{PrismDid => DID}

object RequestNoncesDAO {
  def burn(did: DID, requestNonce: RequestNonce): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO request_nonces (request_nonce, did)
         |VALUES ($requestNonce, $did)
         |ON CONFLICT DO NOTHING
         |RETURNING request_nonce
         |""".stripMargin.query[RequestNonce].option.map {
      case Some(_) => ()
      case None => throw new RuntimeException("The nonce was already used")
    }
  }

  def available(
      did: DID,
      requestNonce: RequestNonce
  ): doobie.ConnectionIO[Boolean] = {
    sql"""
         |SELECT request_nonce
         |FROM request_nonces
         |WHERE request_nonce = $requestNonce AND
         |      did = $did
         |""".stripMargin.query[RequestNonce].option.map(_.isEmpty)
  }
}
