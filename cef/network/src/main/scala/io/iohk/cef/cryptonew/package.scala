package io.iohk.cef

trait Crypto extends cryptonew.HashAlgorithms
                with cryptonew.Hasher
                with cryptonew.EncryptAlgorithms
                with cryptonew.Encrypter
                with cryptonew.DecryptAlgorithms
                with cryptonew.Decrypter

package object cryptonew extends Crypto {

}
