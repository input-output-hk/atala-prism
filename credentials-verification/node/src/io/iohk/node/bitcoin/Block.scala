package io.iohk.node.bitcoin

case class Block(hash: Blockhash, height: Int, time: Long, previous: Option[Blockhash])
