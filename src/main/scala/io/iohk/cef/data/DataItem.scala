package io.iohk.cef.data
import io.iohk.cef.crypto._

trait DataItem {
  def signatures: Seq[(SigningPublicKey, Signature)]
  def owners: Seq[SigningPublicKey]
}
