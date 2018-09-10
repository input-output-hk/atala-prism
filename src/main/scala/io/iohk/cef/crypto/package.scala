package io.iohk.cef

import io.iohk.cef.network.encoding.{Encoder => LowLevelEncoder}
import io.iohk.cef.network.encoding.{Decoder => LowLevelDecoder}
import io.iohk.cef.network.encoding.nio.NioEncoder
import io.iohk.cef.network.encoding.nio.NioDecoder
import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.signing.SigningAlgorithmsCollection
import io.iohk.cef.crypto.encryption.EncryptionAlgorithmsCollection

package object crypto extends Crypto {

  // CONFIGURATION

  private val secureRandom = new java.security.SecureRandom

  protected override val hashingCollection: HashingAlgorithmsCollection =
    HashingAlgorithmsCollection()
  protected override val hashingType: hashingCollection.HashingAlgorithmType =
    hashingCollection.HashingAlgorithmType.KECCAK256

  protected override val encryptionAlgorithmsCollection: EncryptionAlgorithmsCollection =
    EncryptionAlgorithmsCollection(secureRandom)
  protected override val defaultEncryptionType: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
    encryptionAlgorithmsCollection.EncryptionAlgorithmType.RSA

  protected override val signingAlgorithmsCollection: SigningAlgorithmsCollection =
    SigningAlgorithmsCollection(secureRandom)
  protected override val defaultSigningType: signingAlgorithmsCollection.SigningAlgorithmType =
    signingAlgorithmsCollection.SigningAlgorithmType.SHA256withRSA

}

package crypto {

  trait Crypto extends Hashing with Encryption with Signing with EncodingHelpers

  trait EncodingHelpers {

    type Encoder[T] = LowLevelEncoder[T, ByteString]
    type Decoder[T] = LowLevelDecoder[ByteString, T]

    implicit def EncoderFromNIOEncoder[T](implicit nioEncoder: NioEncoder[T]): Encoder[T] =
      (t: T) => ByteString(nioEncoder.encode(t))

    implicit def DecoderFromNIODecoder[T](implicit nioDecoder: NioDecoder[T]): Decoder[T] =
      (u: ByteString) => nioDecoder.decode(u.toByteBuffer)

  }

  sealed trait KeyDecodingError
  object KeyDecodingError {
    case class UnderlayingImplementationError(description: String) extends KeyDecodingError
  }
}
