package io.iohk.cef.ledger.identity
import akka.util.ByteString
import io.iohk.cef.ledger.identity2.IdentityLedgerManager

import scala.concurrent.Future
import scala.util.Try

//In memory implementation. For demoing purposes.
class IdentityLedgerManagerImpl extends IdentityLedgerManager {

  override type Identity = String
  override type PublicKey = ByteString

  override val LedgerId: Int = 1

  override val state: IdentityLedgerState = new IdentityLedgerState {
    var identities: Map[Identity, Set[PublicKey]] = Map()

    override def claim(identity: Identity, key: PublicKey): Future[Unit] = {
      if (identities.contains(identity)) Future.failed(new IllegalArgumentException(s"Identity ${identity} already taken"))
      else Future.successful({
        identities = identities + ((identity, Set(key)))
      })
    }

    override def link(identity: Identity, newKey: PublicKey): Future[Unit] = {
      Future.fromTry(Try {
        identities = identities + ((identity, identities(identity) + (newKey)))
      })
    }

    override def unlink(identity: Identity, key: PublicKey): Future[Unit] = {
      Future.fromTry(Try {
        identities = identities + ((identity, identities(identity) - key))
      })
    }
  }
}
