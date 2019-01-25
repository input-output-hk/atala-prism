package io.iohk.cef.config

import io.iohk.cef.agreements.{AgreementMessage, AgreementsService, AgreementsServiceImpl}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.{ConversationalNetwork, NetworkConfig}

import scala.reflect.runtime.universe._

private[config] class AgreementsServiceBuilder(networkConfig: NetworkConfig) {

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](): AgreementsService[T] = {
    val conversationalNetwork =
      new ConversationalNetwork[AgreementMessage[T]](networkConfig.discovery, networkConfig.transports)
    new AgreementsServiceImpl[T](conversationalNetwork)
  }
}
