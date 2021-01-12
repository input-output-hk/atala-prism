package io.iohk.atala.prism.node.services.models.testing

import io.iohk.atala.prism.node.services.models.AtalaObjectNotification

import scala.collection.mutable
import scala.concurrent.Future

class TestAtalaObjectNotificationHandler {

  val receivedNotifications: mutable.Buffer[AtalaObjectNotification] = mutable.ArrayBuffer()

  def asHandler(notification: AtalaObjectNotification): Future[Unit] = {
    receivedNotifications += notification
    Future.unit
  }
}
