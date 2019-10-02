package io.iohk.node.bitcoin.models

case class BlockHeader(hash: Blockhash, height: Int, time: Long, previous: Option[Blockhash])
