package io.iohk.cef.network
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration

/**
  * @param tcpTransportConfiguration The configuration of the TcpTransport, if one should be used.
  * @param messageTtl The number of network hops before discarding a message.
  */
case class ConversationalNetworkConfiguration(
    tcpTransportConfiguration: Option[TcpTransportConfiguration],
    messageTtl: Int = 5)
