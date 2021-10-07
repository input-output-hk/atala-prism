package io.iohk.atala.prism.node

import io.iohk.atala.prism.node.cardano.models.Lovelace

package object cardano {
  val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"

  // if the available balance is less than this value, then we consider the wallet is unavailable
  val MIN_AVAILABLE_BALANCE_LOVELACE: Lovelace = Lovelace(2000000)

  // According to this https://forum.cardano.org/t/how-to-get-started-with-metadata-on-cardano/45111
  // metadata size is 16kb
  val TX_METADATA_MAX_SIZE: Int = 15000
}
