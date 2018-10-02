package io.iohk.cef.ledger

import akka.util.ByteString
import io.iohk.cef.network.encoding.EncDec

trait ByteStringSerializable[T] extends EncDec[T, ByteString]
