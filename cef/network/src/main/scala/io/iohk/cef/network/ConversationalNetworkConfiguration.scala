package io.iohk.cef.network
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration

case class ConversationalNetworkConfiguration(tcpTransportConfiguration: Option[TcpTransportConfiguration])
