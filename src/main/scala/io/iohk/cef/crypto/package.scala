package io.iohk.cef

import io.iohk.cef.network.encoding.{Encoder => LowLevelEncoder}
import io.iohk.cef.network.encoding.{Decoder => LowLevelDecoder}
import io.iohk.cef.network.encoding.nio.NioEncoder
import io.iohk.cef.network.encoding.nio.NioDecoder
import akka.util.ByteString

/**

USAGE:
  import io.iohk.cef.crypto._

  case class User(name: String, age: Int)
  implicit val userEncoder: Encoder[User] = ??? // Obtained using the appropriate tools/imports

  val user = User("Foo Bar", 42)
  val userHash = user.hash
  val isValid = userHash.isHashOf(user)
  val isNotValid = userHash.isHashOf(User("Bar Foo", 24))

**/
package object crypto extends Crypto {

  // CONFIGURATION

//  private val secureRandom = new java.security.SecureRandom
//
//  override val hashCollection: HashAlgorithmsCollection =
//    HashAlgorithmsCollection()
//  override val hashAlgorithm: hashCollection.HashAlgorithmEntry =
//    hashCollection.KEC256
//
//  override val cryptoCollection: CryptoAlgorithmsCollection =
//    CryptoAlgorithmsCollection(secureRandom)
//  override val cryptoAlgorithm: cryptoCollection.CryptoAlgorithmEntry =
//    cryptoCollection.RSA
//
//  override val signCollection: SignAlgorithmsCollection =
//    SignAlgorithmsCollection(secureRandom)
//  override val signAlgorithm: signCollection.SignAlgorithmEntry =
//    signCollection.RSA

  // HELPERS

  type Encoder[T] = LowLevelEncoder[T, ByteString]
  type Decoder[T] = LowLevelDecoder[ByteString, T]

  implicit def EncoderFromNIOEncoder[T](implicit nioEncoder: NioEncoder[T]): Encoder[T] =
    (t: T) => ByteString(nioEncoder.encode(t))

  implicit def DecoderFromNIODecoder[T](implicit nioDecoder: NioDecoder[T]): Decoder[T] =
    (u: ByteString) => nioDecoder.decode(u.toByteBuffer)
}
