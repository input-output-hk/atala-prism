package io.iohk.node.services

import java.time.Instant

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.modeling._
import shapeless.tag.@@

import scala.concurrent.Future

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait AtalaObjectIdTag
}

package object models {

  import modeltags._

  type AtalaObjectId = SHA256Digest @@ AtalaObjectIdTag
  object AtalaObjectId extends TypeCompanion[SHA256Digest, AtalaObjectIdTag]
  type ReferenceHandler = (SHA256Digest, Instant) => Future[Unit]

}
