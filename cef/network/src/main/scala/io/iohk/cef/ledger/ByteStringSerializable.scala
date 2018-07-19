package io.iohk.cef.ledger

import akka.util.ByteString

trait ByteStringSerializable[T] {

  def serialize(t: T): ByteString

  def deserialize(bytes: ByteString): T
}
