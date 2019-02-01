package io.iohk.cef.frontend.services

import io.iohk.crypto._

class CryptoService() {

  def getSigningKeyPair(): SigningKeyPair =
    generateSigningKeyPair()

}
