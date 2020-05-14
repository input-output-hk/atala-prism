package io.iohk.node.cardano.models

import java.util.Locale

// TODO: Generalize SHA256Value and extend it here (they have different lengths so cannot be done now)
class WalletId private (val string: String) extends AnyVal {
  override def toString: String = string
}

object WalletId {
  private val Pattern = "^[a-f0-9]{40}$".r

  def from(string: String): Option[WalletId] = {
    val lowercaseString = string.toLowerCase(Locale.ROOT)

    lowercaseString match {
      case Pattern() => Some(new WalletId(lowercaseString))
      case _ => None
    }
  }
}
