package io.iohk.query.ledger.identity

import io.iohk.query.ledger.LedgerQuery
import io.iohk.cef.crypto._

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
