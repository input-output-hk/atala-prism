package io.iohk.cef

trait Crypto extends cryptonew.HashAlgorithms
                with cryptonew.Hasher
                with cryptonew.CryptoAlgorithms
                with cryptonew.Crypter
                with cryptonew.SignAlgorithms
                with cryptonew.Signer

package object cryptonew extends Crypto {

}
