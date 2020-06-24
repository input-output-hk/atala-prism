package io.iohk.atala.crypto

import java.security.spec.{ECGenParameterSpec => JavaECGenParameterSpec}
import java.security.{KeyPairGenerator, SecureRandom}

import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * JVM implementation of {@link ECTrait}.
  */
object EC extends ECTrait {
  private val provider = "BC"

  java.security.Security.addProvider(new BouncyCastleProvider)

  override def generateKeyPair(): ECKeyPair = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", provider)
    val ecSpec = new JavaECGenParameterSpec(CURVE_NAME)
    keyGen.initialize(ecSpec, new SecureRandom())
    JvmECKeyPair(keyGen.generateKeyPair())
  }
}
