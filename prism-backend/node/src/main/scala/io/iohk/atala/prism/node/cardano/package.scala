package io.iohk.atala.prism.node

package object cardano {
  val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"

  // According to this https://forum.cardano.org/t/how-to-get-started-with-metadata-on-cardano/45111
  // metadata size is 16kb
  val TX_METADATA_MAX_SIZE: Int = 15000
}
