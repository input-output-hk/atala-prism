package io.iohk.atala.prism.node

package object cardano {
  val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"

  // According to this https://forum.cardano.org/t/how-to-get-started-with-metadata-on-cardano/45111
  // metadata size is 16kb
  /* TODO: this here might not be correct because
   * it is used to check against transaction metadata string **length** without spaces
   * strings in scala take 2 bytes per character (roughly) without considering other factors
   * that make affect the amount of bytes one string takes. so 16 kilobytes -> 16 000 bytes,
   * that would hold a string with length of 8000, not 15000
   */
  val TX_METADATA_MAX_SIZE: Int = 15000
}
