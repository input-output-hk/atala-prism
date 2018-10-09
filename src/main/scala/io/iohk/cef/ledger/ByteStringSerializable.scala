package io.iohk.cef.ledger

import akka.util.ByteString
import io.iohk.cef.network.encoding.EncoderDecoder

trait ByteStringSerializable[T] extends EncoderDecoder[T, ByteString]
