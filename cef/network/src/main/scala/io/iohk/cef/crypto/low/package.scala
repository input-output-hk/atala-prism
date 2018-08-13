package io.iohk.cef.crypto

trait CryptoLow extends low.HashAlgorithmPackageFragment
                   with low.HasherPackageFragment
                   with low.CryptoAlgorithmPackageFragment
                   with low.CrypterPackageFragment
                   with low.SignAlgorithmPackageFragment
                   with low.SignerPackageFragment

/**
  * Collection of low-level cryptographic primitives
  */
package object low extends CryptoLow {

}
