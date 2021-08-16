package io.iohk.atala.cvp.webextension.common

import typings.bip39.{mod => bip39}
import typings.node.bufferMod.global.{Buffer, BufferEncoding}

import scala.concurrent.Future

case class Mnemonic(seed: String) {
  def toHex: String = {
    bip39.mnemonicToSeedSync(seed).toString(BufferEncoding.hex)
  }
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
    require(isValid(seed))
    new Mnemonic(seed)
  }

  def isValid(seed: String): Boolean = {
    bip39.validateMnemonic(seed.toLowerCase.split("\\s+").mkString(" "))
  }
}
