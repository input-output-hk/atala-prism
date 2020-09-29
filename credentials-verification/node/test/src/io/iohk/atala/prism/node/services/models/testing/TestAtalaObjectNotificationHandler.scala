package io.iohk.atala.prism.node.services.models.testing

import io.iohk.atala.prism.node.services.models.AtalaObjectNotification

import scala.collection.mutable
import scala.concurrent.Future

class TestAtalaObjectNotificationHandler {

  val receivedNotifications: mutable.MutableList[AtalaObjectNotification] = mutable.MutableList()

  def asHandler(notification: AtalaObjectNotification): Future[Unit] = {
    receivedNotifications += notification
    Future.unit
  }
}
