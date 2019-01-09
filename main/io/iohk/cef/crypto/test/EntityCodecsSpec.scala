package io.iohk.cef.crypto

import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.test.utils.CodecTestingHelpers
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.test.utils.CryptoEntityArbitraries

class EntityCodecsSpec extends FlatSpec with CodecTestingHelpers with CryptoEntityArbitraries {

  behavior of "CryptoEntities Codecs"

  it should "work correctly with TypedByteString" in { testWhenNotEncodingType[TypedByteString] }
  it should "work correctly with SigningPublicKey" in { testWhenNotEncodingType[SigningPublicKey] }
  it should "work correctly with SigningPrivateKey" in { testWhenNotEncodingType[SigningPrivateKey] }
  it should "work correctly with SigningKeyPair" in { testWhenNotEncodingType[SigningKeyPair] }
  it should "work correctly with EncryptionPublicKey" in { testWhenNotEncodingType[EncryptionPublicKey] }
  it should "work correctly with EncryptionPrivateKey" in { testWhenNotEncodingType[EncryptionPrivateKey] }
  it should "work correctly with EncryptionKeyPair" in { testWhenNotEncodingType[EncryptionKeyPair] }
  it should "work correctly with Hash" in { testWhenNotEncodingType[Hash] }
  it should "work correctly with Signature" in { testWhenNotEncodingType[Signature] }
  it should "work correctly with EncryptedData" in { testWhenNotEncodingType[EncryptedData] }

}
