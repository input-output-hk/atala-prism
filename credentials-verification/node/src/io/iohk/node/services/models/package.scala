package io.iohk.node.services

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.modeling._
import shapeless.tag.@@

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait AtalaObjectIdTag
}

package object models {

  import modeltags._

  type AtalaObjectId = SHA256Digest @@ AtalaObjectIdTag
  object AtalaObjectId extends TypeCompanion[SHA256Digest, AtalaObjectIdTag]

}
