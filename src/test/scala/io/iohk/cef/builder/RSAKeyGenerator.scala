package io.iohk.cef.builder

import java.security.{PrivateKey, PublicKey}

import io.iohk.cef.crypto.low.CryptoAlgorithm

trait RSAKeyGenerator extends SecureRandomBuilder {

  private val crypto = new CryptoAlgorithm.RSA(secureRandom)

  def generateKeyPair: (PublicKey, PrivateKey) = crypto.generateKeyPair
}
