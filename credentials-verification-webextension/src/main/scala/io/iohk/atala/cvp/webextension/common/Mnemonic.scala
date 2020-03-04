package io.iohk.atala.cvp.webextension.common

import typings.bip39.{mod => bip39}

case class Mnemonic(seed: String) {
  def toHex: String = {
    bip39.mnemonicToSeedSync(seed).toString("hex")
  }
}
object Mnemonic {
  def apply(): Mnemonic = new Mnemonic(bip39.generateMnemonic())

  def apply(seed: String): Mnemonic = {
    require(bip39.validateMnemonic(seed))
    new Mnemonic(seed)
  }
}
