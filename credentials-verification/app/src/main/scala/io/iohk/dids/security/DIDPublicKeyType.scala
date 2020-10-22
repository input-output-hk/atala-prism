package io.iohk.dids.security

sealed trait DIDPublicKeyType {
  protected lazy val algorithmsSet = algorithms.toSet

  def algorithms: List[String]

  def name: String

  def hasAlgorithm(name: String): Boolean = algorithmsSet.contains(name)
}

sealed trait DIDECPublicKeyType extends DIDPublicKeyType {
  def curve: String
}

object Secp256k1VerificationKey2018 extends DIDECPublicKeyType {
  override val name = "Secp256k1VerificationKey2018"

  override val algorithms: List[String] = List("ES256K", "ES256K-R")

  override val curve = "P256-K"
}
