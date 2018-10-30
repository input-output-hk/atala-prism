package io.iohk.cef.ledger

import akka.util.ByteString
import io.iohk.cef.codecs.EncoderDecoder

trait ByteStringSerializable[T] extends EncoderDecoder[T, ByteString]
