package io.iohk.node.services

import shapeless.tag.@@
import io.iohk.node.modeling._

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait AtalaObjectIdTag
}

package object models {

  import modeltags._

  type AtalaObjectId = Array[Byte] @@ AtalaObjectIdTag
  object AtalaObjectId extends TypeCompanion[Array[Byte], AtalaObjectIdTag]

}
