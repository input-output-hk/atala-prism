package io.iohk.node

import java.time.Instant

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.services.models.ReferenceHandler

import scala.concurrent.Future

trait AtalaReferenceLedger {
  def publishReference(ref: SHA256Digest): Future[Unit]
}

trait BlockchainSynchronizerFactory {
  def apply(onNewReference: ReferenceHandler): AtalaReferenceLedger
}

class InMemoryAtalaReferenceLedger(onNewReference: ReferenceHandler) extends AtalaReferenceLedger {
  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    onNewReference(ref, Instant.now())
  }
}
