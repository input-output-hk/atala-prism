package io.iohk.atala.prism.node.services

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.modeling._
import shapeless.tag.@@
import io.iohk.prism.protos.node_internal

import scala.concurrent.Future

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait AtalaObjectIdTag
}

package object models {

  import modeltags._

  type AtalaObjectId = SHA256Digest @@ AtalaObjectIdTag
  object AtalaObjectId extends TypeCompanion[SHA256Digest, AtalaObjectIdTag]

  case class AtalaObjectNotification(
      atalaObject: node_internal.AtalaObject,
      timestamp: Instant,
      transaction: TransactionInfo
  )

  type AtalaObjectNotificationHandler = AtalaObjectNotification => Future[Unit]
}
