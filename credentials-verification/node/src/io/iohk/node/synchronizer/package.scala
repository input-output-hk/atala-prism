package io.iohk.node

import io.iohk.node.bitcoin.models.Blockhash

import scala.concurrent.duration.FiniteDuration

package object synchronizer {

  case class BlockPointer(blockhash: Blockhash, height: Int)

  case class SynchronizerConfig(delay: FiniteDuration)
}
