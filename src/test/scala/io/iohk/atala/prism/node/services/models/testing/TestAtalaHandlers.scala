package io.iohk.atala.prism.node.services.models.testing

import cats.Applicative
import io.iohk.atala.prism.node.cardano.models.Block.Canonical
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import scala.collection.mutable

class TestAtalaHandlers[F[_]: Applicative] {

  val receivedNotifications: mutable.Buffer[AtalaObjectNotification] =
    mutable.ArrayBuffer()
  val receivedAtalaObjectBulk: mutable.Buffer[List[AtalaObjectNotification]] =
    mutable.ArrayBuffer()

  def asAtalaObjectHandler(
      notification: AtalaObjectNotification
  ): F[Unit] = {
    receivedNotifications += notification
    Applicative[F].unit
  }

  val receivedCardanoBlocks: mutable.Buffer[Canonical] = mutable.ArrayBuffer()

  def asCardanoBlockHandler(block: Canonical): F[Unit] = {
    receivedCardanoBlocks += block
    Applicative[F].unit
  }

  def asAtalaObjectBulkHandler(notifications: List[AtalaObjectNotification]): F[Unit] = {
    receivedAtalaObjectBulk += notifications
    Applicative[F].unit
  }
}
