package io.iohk.atala.prism.node.bitcoin.models

case class BlockHeader(hash: Blockhash, height: Int, time: Long, previous: Option[Blockhash])
