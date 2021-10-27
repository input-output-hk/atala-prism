package io.iohk.atala.prism.node.cardano.models

import java.time.Instant

case class BlockHeader(
    hash: BlockHash,
    blockNo: Int,
    time: Instant,
    previousBlockHash: Option[BlockHash]
)
