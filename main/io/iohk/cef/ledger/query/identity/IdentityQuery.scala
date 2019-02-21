package io.iohk.cef.ledger.query.identity

import io.iohk.cef.ledger.identity.Identity
import io.iohk.cef.ledger.query.LedgerQuery
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

  case class RetrieveEndorsements(identity: Identity) extends IdentityQuery {
    override type Response = Set[Identity]

    override protected def perform(queryEngine: IdentityQueryEngine): Set[Identity] = {
      queryEngine
        .keys()
        .flatMap { key =>
          queryEngine
            .get(key)
            .filter(_.endorsers contains identity)
            .map(key -> _)
        }
        .map(_._1)
    }
  }

  case object RetrieveIdentities extends IdentityQuery {
    override type Response = Set[Identity]

    override protected def perform(queryEngine: IdentityQueryEngine): Set[Identity] = {
      queryEngine.keys()
    }
  }
}
