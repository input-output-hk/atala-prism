package io.iohk.atala.cvp.webextension.facades.bn

import scala.scalajs.js

@js.native
trait BigNumber extends js.Object {

  override def clone(): BigNumber = js.native

  def toString(base: Int): String = js.native
  def toString(base: Int, length: Int): String = js.native
  def toString(encoding: String): String = js.native
  def toNumber(): Long = js.native

  def bitLength: Int = js.native
}

@js.native
trait ReducedBigNumber extends js.Object {
  def toString(base: Int): String = js.native
  def toString(base: Int, length: Int): String = js.native
  def toString(encoding: String): String = js.native
}
