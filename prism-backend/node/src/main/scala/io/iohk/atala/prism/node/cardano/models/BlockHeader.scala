package io.iohk.atala.prism.node.cardano.models

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(loggable)
case class BlockHeader(
    hash: BlockHash,
    blockNo: Int,
    time: Instant,
    previousBlockHash: Option[BlockHash]
)
