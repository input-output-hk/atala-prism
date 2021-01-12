package io.iohk.atala.prism.node.cardano

import io.iohk.atala.prism.node.modeling._
import shapeless.tag.@@

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait LovelaceTag
  sealed trait AddressTag
}

package object models {
  import modeltags._

  type Lovelace = BigInt @@ LovelaceTag
  object Lovelace extends TypeCompanion[BigInt, LovelaceTag]

  type Address = String @@ AddressTag
  object Address extends TypeCompanion[String, AddressTag]
}
