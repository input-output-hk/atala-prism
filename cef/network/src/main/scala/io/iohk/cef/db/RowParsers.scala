package io.iohk.cef.db

import java.net.{InetAddress, InetSocketAddress}
import java.time.Instant

import akka.util.ByteString
import anorm._
import org.bouncycastle.util.encoders.Hex

object RowParsers {

  def byteStringParser(columnName: String): RowParser[ByteString] =
    SqlParser.str(columnName).map(hex => ByteString(Hex.decode(hex)))

  def inetAddressParser(columnName: String): RowParser[InetAddress] = SqlParser.byteArray(columnName).map(InetAddress.getByAddress)

  def inetSocketAddressParser(addressColumnName: String, portColumnName: String): RowParser[InetSocketAddress] =
    inetAddressParser(addressColumnName) ~ SqlParser.int(portColumnName) map {
      case inetAddr ~ int => new InetSocketAddress(inetAddr, int)
    }

  def instant(columnName: String)(implicit c: Column[Instant]): RowParser[Instant] = {
    SqlParser.get[Instant](columnName)(c)//.map(_.toInstant)
  }
}
