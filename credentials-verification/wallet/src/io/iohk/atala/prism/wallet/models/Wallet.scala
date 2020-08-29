package io.iohk.atala.prism.wallet.models

import java.security.{PrivateKey, PublicKey}

case class Wallet(did: String, privateKey: PrivateKey, publicKey: PublicKey)
