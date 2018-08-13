package io.iohk.cef.crypto.low

import akka.util.ByteString

/**
  * Collection of signing algorithms
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait SignAlgorithmPackageFragment {

  /**
    * Contract all signing algorithm implementations should follow
    */
  sealed trait SignAlgorithm {


    /**
      * Computes the signature for the provided `source` bytes, using the provided
      * `key`
      *
      * @param source     the bytes to be signed
      * @param key        the cryptographic key to be used to perform the signature
      *
      * @return a cryptographic signature for the provided `source` bytes
      */
    def sign(source: ByteString, key: ByteString): ByteString

    /**
      * Validates the `signature` of the provided `source` bytes, using the provided `key`
      *
      * @param signature  the signature that need to be checked against `source`
      * @param source     the bytes that need to be validated against the `signature`
      * @param key        the cryptographic key to be used to perform the validation
      *
      * @return `true` if the `signature` corresponds to the `source` when validated using
      *         the provided `key`. `false` otherwise
      */
    def validate(signature: ByteString, source: ByteString, key: ByteString): Boolean

  }

  /**
    * Helper trait that allows the implementation of a `SignAlgorithm` basing it on
    * `Array[Byte]` instead of `ByteString`
    */
  sealed trait ArrayBasedSignAlgorithm extends SignAlgorithm {

    /**
      * Computes the signature for the provided `source` bytes, using the provided
      * `key`
      *
      * @param source     the bytes to be signed
      * @param key        the cryptographic key to be used to perform the signature
      *
      * @return a cryptographic signature for the provided `source` bytes
      */
    protected def sign(source: Array[Byte], key: Array[Byte]): Array[Byte]

    /** @inheritdoc */
    override final def sign(source: ByteString, key: ByteString): ByteString =
      ByteString(sign(source.toArray, key.toArray))

    /**
      * Validates the `signature` of the provided `source` bytes, using the provided `key`
      *
      * @param signature  the signature that need to be checked against `source`
      * @param source     the bytes that need to be validated against the `signature`
      * @param key        the cryptographic key to be used to perform the validation
      *
      * @return `true` if the `signature` corresponds to the `source` when validated using
      *         the provided `key`. `false` otherwise
      */
    protected def validate(signature: Array[Byte], source: Array[Byte], key: Array[Byte]): Boolean

    /** @inheritdoc */
    override final def validate(signature: ByteString, source: ByteString, key: ByteString): Boolean =
      validate(signature.toArray, source.toArray, key.toArray)

  }

  /**
    * Companion object to SignAlgorithm, containing all the implemented `SignAlgorithm`
    */
  object SignAlgorithm {

    /**
      * `SignAlgorithm` based on the composition of a `CryptoAlgorithm` and a `HashAlgorithm`
      *
      * @param cryptoAlgorithm used to encrypt the signature, allowing the validation of the
      *                        signature author
      *
      * @param hashAlgorithm used to ensure the content of the message has not been medled
      *        with
      */
    case class Composed(cryptoAlgorithm: CryptoAlgorithm, hashAlgorithm: HashAlgorithm) extends SignAlgorithm {

      /** @inheritdoc */
      def sign(source: ByteString, key: ByteString): ByteString =
        cryptoAlgorithm.encrypt(
          hashAlgorithm(source),
          key)

      /** @inheritdoc */
      def validate(signature: ByteString, source: ByteString, key: ByteString): Boolean =
        cryptoAlgorithm.decrypt(signature, key) == hashAlgorithm(source)
    }

  }

}

