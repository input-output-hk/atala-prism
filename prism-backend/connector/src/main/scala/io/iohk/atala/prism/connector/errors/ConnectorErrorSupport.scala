package io.iohk.atala.prism.connector.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait ConnectorErrorSupport extends ErrorSupport[ConnectorError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)

  override def invalidRequest(message: String): ConnectorError =
    InvalidRequest(message)
}

trait ConnectorErrorSupportNew[E <: ConnectorError] extends ErrorSupport[E] {
  def fromConnectorError(ce: ConnectorError): E

  override def wrapAsServerError(cause: Throwable): E = fromConnectorError(InternalServerError(cause))

  override def invalidRequest(message: String): E = fromConnectorError(InvalidRequest(message))

  def unknownValueError(tpe: String, value: String): E = fromConnectorError(UnknownValueError(tpe, value))

  def invalidArgumentError(tpe: String, requirement: String, value: String): E = fromConnectorError(InvalidArgumentError(tpe, requirement, value))
}
