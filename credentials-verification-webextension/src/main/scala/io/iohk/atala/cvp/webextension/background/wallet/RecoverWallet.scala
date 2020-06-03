package io.iohk.atala.cvp.webextension.background.wallet

import java.util.{Base64, UUID}

import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, EcKeyPair, Mnemonic}
import io.iohk.prism.protos.connector_api.GetCurrentUserRequest
import typings.elliptic.mod.ec.Signature
import typings.hashJs.{hashJsStrings, mod => hash}

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.{Uint8Array, _}

class RecoverWallet(mnemonic: Mnemonic) {

  val ecKeyPair: EcKeyPair = ECKeyOperation.createECKeyPair(mnemonic)

  val atalaOperation = ECKeyOperation.createDIDOperation(ecKeyPair)

  def createDIDId(): String = {
    val byteArray = atalaOperation.toByteArray.toTypedArray
    val uint8Array = new Uint8Array(byteArray.buffer, byteArray.byteOffset, byteArray.length)
    val sha256 = hash.sha256().update(uint8Array)
    val didSuffix = sha256.digest_hex(hashJsStrings.hex)
    val did = s"did:prism:$didSuffix"
    did
  }

  def getUrlEncodedDIDSignature(requestNonce: RequestNonce, request: GetCurrentUserRequest): String = {
    val mergedBytes = requestNonce.bytes ++ request.toByteArray
    val sha256 = hash.sha256().update(mergedBytes.toJSArray.asInstanceOf[Uint8Array])
    val hashedSignature = sha256.digest_hex(hashJsStrings.hex)
    val signature: Signature = ecKeyPair.privateKeyPair.sign(hashedSignature)
    Base64.getUrlEncoder.encodeToString(signature.toDER().asInstanceOf[Int8Array].toArray)
  }

  def getUrlEncodedRequestNonce(requestNonce: RequestNonce): String = {
    Base64.getUrlEncoder.encodeToString(requestNonce.bytes)
  }

  def requestNonce(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }

}

object RecoverWallet {
  def apply(mnemonic: Mnemonic): RecoverWallet = new RecoverWallet(mnemonic)
}

case class RequestNonce(bytes: Array[Byte]) extends AnyVal
