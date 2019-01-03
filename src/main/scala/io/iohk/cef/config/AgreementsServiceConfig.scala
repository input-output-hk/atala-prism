package io.iohk.cef.config

import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.codecs.nio._

import scala.reflect.runtime.universe._

private[config] class AgreementsServiceConfig(
    cefConfig: CefConfig) {

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](): AgreementsService[T] = {

    new AgreementsService[T](cefConfig.peerConfig)
  }
}
