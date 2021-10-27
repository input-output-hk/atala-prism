package io.iohk.atala.prism.node.services.models.testing

import io.iohk.atala.prism.node.cardano.models.Block.Canonical
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification

import scala.collection.mutable
import scala.concurrent.Future

class TestAtalaHandlers {

  val receivedNotifications: mutable.Buffer[AtalaObjectNotification] =
    mutable.ArrayBuffer()

  def asAtalaObjectHandler(
      notification: AtalaObjectNotification
  ): Future[Unit] = {
    receivedNotifications += notification
    Future.unit
  }

  val receivedCardanoBlocks: mutable.Buffer[Canonical] = mutable.ArrayBuffer()

  def asCardanoBlockHandler(block: Canonical): Future[Unit] = {
    receivedCardanoBlocks += block
    Future.unit
  }
}
