package io.iohk.cef.network
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration

case class ConversationalNetworkConfiguration( /**
                                                * The configuration of the TcpTransport, if one should be used.
                                                */
                                              tcpTransportConfiguration: Option[TcpTransportConfiguration],
                                              /**
                                                * The number of network hops before discarding a message.
                                                */
                                              messageTtl: Int = 5)
