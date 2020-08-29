package io.iohk.atala.prism.node

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.services.models.{AtalaObjectUpdate, ObjectHandler}

import scala.concurrent.Future

trait AtalaReferenceLedger {
  def supportsOnChainData: Boolean
  def publishReference(ref: SHA256Digest): Future[Unit]
  def publishObject(bytes: Array[Byte]): Future[Unit]
}

trait BlockchainSynchronizerFactory {
  def apply(onNewObject: ObjectHandler): AtalaReferenceLedger
}

class InMemoryAtalaReferenceLedger(onNewObject: ObjectHandler) extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = true

  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    onNewObject(AtalaObjectUpdate.Reference(ref), Instant.now())
  }

  override def publishObject(bytes: Array[Byte]): Future[Unit] = {
    onNewObject(AtalaObjectUpdate.ByteContent(bytes), Instant.now())
  }
}
