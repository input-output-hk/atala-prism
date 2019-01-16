package io.iohk.cef.config

import java.net.{InetSocketAddress, URI}

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, NodeId}
import io.iohk.cef.utils.HexStringCodec.fromHexString
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

object ConfigReaderExtensions {

  implicit val inetSocketAddressReader: ConfigReader[InetSocketAddress] =
    ConfigReader.fromString[InetSocketAddress] { s =>
      val uri = new URI(s"dummy://$s")
      if (uri.getHost == null || uri.getPort == -1)
        Left(CannotConvert(s, "InetSocketAddress", "Expected a string with format <host>:<port> or <ip>:<port>."))
      else
        Right(new InetSocketAddress(uri.getHost, uri.getPort))
    }

  implicit val byteStringReader: ConfigReader[ByteString] = ConfigReader[String].map(fromHexString)

  implicit val capabilitiesReader: ConfigReader[Capabilities] = ConfigReader[String].map(s => Capabilities(s.toByte))

  implicit val nodeIdReader: ConfigReader[NodeId] = ConfigReader[String].map(s => NodeId(s))
}
