package io.iohk.cef.frontend.services

import io.iohk.cef.crypto._

class CryptoService() {

  def getSigningKeyPair(): SigningKeyPair =
    generateSigningKeyPair()

}
