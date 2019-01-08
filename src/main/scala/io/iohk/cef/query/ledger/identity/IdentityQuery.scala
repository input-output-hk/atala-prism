package io.iohk.cef.query.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.query.ledger.LedgerQuery

sealed trait IdentityQuery extends LedgerQuery[IdentityPartition]

case class RetrieveIdentityKeys(identity: String) extends IdentityQuery {
  type Response = Set[SigningPublicKey]
  override protected def perform(queryEngine: IdentityQueryEngine): Set[SigningPublicKey] =
    queryEngine.get(identity).getOrElse(Set.empty)
}

case class ExistsIdentity(identity: String) extends IdentityQuery {
  type Response = Boolean
  override protected def perform(queryEngine: IdentityQueryEngine): Boolean =
    queryEngine.contains(identity)
}
