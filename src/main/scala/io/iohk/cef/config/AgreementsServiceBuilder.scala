package io.iohk.cef.config

import io.iohk.cef.agreements.{AgreementMessage, AgreementsService}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.ConversationalNetwork
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports

import scala.reflect.runtime.universe._

private[config] class AgreementsServiceBuilder(
    cefConfig: CefConfig,
    transports: Transports,
    networkDiscovery: NetworkDiscovery
) {

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](): AgreementsService[T] = {
    new AgreementsService[T](new ConversationalNetwork[AgreementMessage[T]](networkDiscovery, transports))
  }
}
