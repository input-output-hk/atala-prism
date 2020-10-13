package io.iohk.atala.crypto

import typings.hashJs.{mod => hash}

import scala.scalajs.js.typedarray.{Uint8Array, _}

private[crypto] object SHA256DigestImpl {
  def compute(bytes: Array[Byte]): Array[Byte] = {
    val byteArray = bytes.toTypedArray
    val uint8Array = new Uint8Array(byteArray.buffer, byteArray.byteOffset, byteArray.length)
    val sha256 = hash.sha256().update(uint8Array)
    sha256.digest().toArray map (_.toByte)
  }
}
