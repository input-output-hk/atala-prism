package io.iohk.cef.query.ledger.identity

import io.iohk.cef.ledger.identity.Identity
import io.iohk.cef.query.ledger.LedgerQuery
import io.iohk.crypto._

sealed trait IdentityQuery extends LedgerQuery[IdentityPartition]

object IdentityQuery {

  case class RetrieveIdentityKeys(identity: Identity) extends IdentityQuery {
    type Response = Set[SigningPublicKey]

    override protected def perform(queryEngine: IdentityQueryEngine): Response = {
      queryEngine
        .get(identity)
        .map(_.keys)
        .getOrElse(Set.empty)
    }
  }

  case class ExistsIdentity(identity: Identity) extends IdentityQuery {
    type Response = Boolean

    override protected def perform(queryEngine: IdentityQueryEngine): Response =
      queryEngine.contains(identity)
  }

  case class RetrieveEndorsers(identity: Identity) extends IdentityQuery {
    override type Response = Set[Identity]

    override protected def perform(queryEngine: IdentityQueryEngine): Response = {
      queryEngine
        .get(identity)
        .map(_.endorsers)
        .getOrElse(Set.empty)
    }
  }
}
