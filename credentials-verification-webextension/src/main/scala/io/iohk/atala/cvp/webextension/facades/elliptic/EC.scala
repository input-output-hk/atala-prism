package io.iohk.atala.cvp.webextension.facades.elliptic

import io.iohk.atala.cvp.webextension.facades.bn.{BigNumber, ReducedBigNumber}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("elliptic", "ec", globalFallback = "elliptic")
class EC(curveName: String) extends js.Object {
  def genKeyPair(): KeyPair = js.native
  //def sign(keys: KeyPair, encoding: String): String = js.native // TODO: options
  def verify(message: String, signature: Signature, key: KeyPair): Boolean = js.native // TODO: types of signature?
}
@js.native
trait KeyPair extends js.Object {
  def getPublic(): CurvePoint = js.native
  def getPublic(encoding: String): String = js.native
  def getPrivate(): BigNumber = js.native
  def getPrivate(encoding: String): String = js.native

  def sign(message: Array[Byte]): Signature = js.native
  def verify(message: Array[Byte], signature: Signature): Boolean = js.native

  /** Signs hex-encoded message */
  def sign(message: String): Signature = js.native
  def verify(message: String, signature: Signature): Boolean = js.native
}

/** ECDSA signature
  *
  * @param r hex encoded r
  * @param s hex encoded s
  */
@js.native
@JSImport("elliptic", "Signature")
class Signature(r: String, s: String) extends js.Object {
  def r: ReducedBigNumber = js.native
  def s: ReducedBigNumber = js.native
  def toDER(encoding: String): String = js.native
}

@js.native
trait CurvePoint extends js.Object {
  def getX(): ReducedBigNumber = js.native
  def getY(): ReducedBigNumber = js.native
  def encode(format: String): String = js.native
}
