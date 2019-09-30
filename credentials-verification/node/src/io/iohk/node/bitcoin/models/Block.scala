package io.iohk.node.bitcoin.models

case class Block(hash: Blockhash, height: Int, time: Long, previous: Option[Blockhash])
