package io.iohk.cef.ledger

import akka.util.ByteString
import io.iohk.cef.network.encoding.{Decoder, Encoder}

trait ByteStringSerializable[T] extends Encoder[T, ByteString] with Decoder[ByteString, T]
