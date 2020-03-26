package io.iohk.node

import io.iohk.cvp.crypto.SHA256Digest

import scala.concurrent.Future

trait AtalaReferenceLedger {
  def publishReference(ref: SHA256Digest): Future[Unit]
}

trait BlockchainSynchronizerFactory {
  def apply(onNewReference: SHA256Digest => Future[Unit]): AtalaReferenceLedger
}

class InMemoryAtalaReferenceLedger(onNewReference: SHA256Digest => Future[Unit]) extends AtalaReferenceLedger {
  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    onNewReference(ref)
  }
}
