package io.iohk.atala.crypto

/**
  * Trait that implements all shared behavior between js/EC and jvm/EC.
  *
  * <p>Client code should use `EC` without thinking on which implementation (JS, JVM) is being used. This will allow
  * downstream cross-compiled projects to work seamlessly.
  */
private[crypto] trait ECTrait {
  protected val CURVE_NAME = "secp256k1"

  /**
    * Generates a P-256k/secp256k1/prime256v1 key-pair.
    */
  def generateKeyPair(): ECKeyPair

  // TODO: Define more behavior.
}
