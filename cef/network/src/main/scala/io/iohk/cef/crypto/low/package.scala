package io.iohk.cef.crypto

trait CryptoLow extends low.HashAlgorithms
                   with low.Hasher
                   with low.CryptoAlgorithms
                   with low.Crypter
                   with low.SignAlgorithms
                   with low.Signer

/**
  * Collection of low-level cryptographic primitives
  */
package object low extends CryptoLow {

}
