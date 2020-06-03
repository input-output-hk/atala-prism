package io.iohk.atala.cvp.webextension.common

import typings.bip39.{mod => bip39}
import typings.node.Buffer

import scala.concurrent.Future

case class Mnemonic(seed: String) {
  def toSyncBuffer: Buffer = {
    bip39.mnemonicToSeedSync(seed)
  }
  def toBuffer: Future[Buffer] = {
    bip39.mnemonicToSeed(seed).toFuture
  }
}
object Mnemonic {
  def apply(): Mnemonic = new Mnemonic(bip39.generateMnemonic())

  def apply(seed: String): Mnemonic = {
    require(bip39.validateMnemonic(seed))
    new Mnemonic(seed)
  }
}
