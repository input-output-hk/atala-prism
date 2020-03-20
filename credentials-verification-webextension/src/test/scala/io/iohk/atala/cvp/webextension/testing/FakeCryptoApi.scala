package io.iohk.atala.cvp.webextension.testing

import org.scalajs.dom.crypto.{
  AlgorithmIdentifier,
  BufferSource,
  CryptoKey,
  KeyAlgorithmIdentifier,
  KeyFormat,
  KeyUsage
}

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

object FakeCryptoApi extends js.Object {
  val subtle: js.Object = new js.Object() {
    def importKey(
        format: KeyFormat,
        keyData: BufferSource,
        algorithm: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]
    ): js.Promise[js.Any] = {
      js.Promise.resolve[js.Any](FakeCryptoKey)
    }

    def deriveKey(
        algorithm: AlgorithmIdentifier,
        baseKey: CryptoKey,
        derivedKeyType: KeyAlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: js.Array[KeyUsage]
    ): js.Promise[js.Any] = {
      js.Promise.resolve[js.Any](FakeCryptoKey)
    }

    def encrypt(algorithm: AlgorithmIdentifier, key: CryptoKey, data: BufferSource): js.Promise[js.Any] = {
      shiftBufferSource(data, 1)
      js.Promise.resolve[js.Any](data)
    }

    def decrypt(algorithm: AlgorithmIdentifier, key: CryptoKey, data: BufferSource): js.Promise[js.Any] = {
      shiftBufferSource(data, -1)
      js.Promise.resolve[js.Any](data)
    }

    def shiftBufferSource(data: BufferSource, delta: Int): Unit = {
      val dataArray = new Uint8Array(data.asInstanceOf[ArrayBuffer])
      for (i <- 0 until dataArray.length) {
        dataArray.set(i, (dataArray.get(i) + delta).toByte)
      }
    }
  }

  object FakeCryptoKey extends js.Object {
    val `type` = "test"

    val extractable = true

    val algorithm: js.Object = new js.Object {
      var name = "test-algorithm"
    }

    val usages: js.Array[js.Object] = js.Array()
  }
}
