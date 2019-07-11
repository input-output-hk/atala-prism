package atala.config

import pureconfig.error._
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.DomainValidator

trait ServerAddressValidator {
  def validateServerAddress(candidate: ServerAddress): Option[FailureReason]
}

object ServerAddressValidator {
  private val ipValidator = InetAddressValidator.getInstance()
  private val hostnameValidator = DomainValidator.getInstance(true)
  private val portValidator = PortValidator()

  def apply(): ServerAddressValidator = new ServerAddressValidatorImpl(ipValidator, hostnameValidator, portValidator)
}

class ServerAddressValidatorImpl(
    ipValidator: InetAddressValidator,
    hostnameValidator: DomainValidator,
    portValidator: PortValidator
) extends ServerAddressValidator {

  def isValidPort(candidate: Int): Option[FailureReason] =
    portValidator.isValid(candidate) match {
      case invalid: PortValidation.InvalidPort =>
        Some(
          CannotConvert(
            candidate.toString,
            "ServerAddress",
            invalid.description(candidate)
          )
        )
      case PortValidation.ValidPort =>
        None
    }

  def isValidHostname(candidate: String): Boolean =
    hostnameValidator.isValid(candidate)

  def isValidIp(candidate: String): Boolean =
    ipValidator.isValid(candidate)

  def isValidHost(candidate: String): Option[FailureReason] = {
    if (isValidHostname(candidate) || isValidIp(candidate)) None
    else
      Some(
        CannotConvert(
          candidate.toString,
          "ServerAddress",
          s"The configured host '${candidate}' is neither a valid hostname nor a valid IP address"
        )
      )

  }

  override def validateServerAddress(candidate: ServerAddress): Option[FailureReason] =
    isValidPort(candidate.port) orElse isValidHost(candidate.host)

}

case class PortValidator() {
  // Using this article as reference for valid ports: https://en.wikipedia.org/wiki/Registered_port
  def isValid(candidate: Int): PortValidation =
    if (candidate < 0)
      PortValidation.NegativePort
    else if (candidate < 1024)
      PortValidation.ReservedPort
    else if (candidate > 49151)
      PortValidation.DynamicPort
    else
      PortValidation.ValidPort
}

sealed trait PortValidation
object PortValidation {
  case object ValidPort extends PortValidation
  sealed abstract class InvalidPort(val description: Int => String) extends PortValidation

  case object NegativePort
      extends InvalidPort(
        port => s"The configured port number ${port} should be positive (and between 1024 and 49151 inclusive)"
      )
  case object ReservedPort
      extends InvalidPort(
        port =>
          s"The configured port number ${port} is in the range of ports reserved by the OS. It should be between 1024 and 49151, inclusive"
      )
  case object DynamicPort
      extends InvalidPort(
        port =>
          s"The configured port number ${port} is in the range reserved for dynamic and private ports. It should be between 1024 and 49151, inclusive"
      )
}
