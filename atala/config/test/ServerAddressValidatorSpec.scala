package atala.config
package test

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues._
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.DomainValidator
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import PortValidation._

class ServerAddressValidatorSpec extends WordSpec with MustMatchers with MockitoSugar {

  def genValidator(validIp: Boolean, validHostname: Boolean, portValidation: PortValidation): ServerAddressValidator = {
    val ipValidator = new InetAddressValidator() {
      override def isValid(inetAddress: String): Boolean = validIp
    }
    val hostnameValidator = mock[DomainValidator]
    when(hostnameValidator.isValid("")).thenReturn(validHostname)
    val portValidator = new PortValidator() {
      override def isValid(candidate: Int): PortValidation = portValidation
    }
    new ServerAddressValidatorImpl(ipValidator, hostnameValidator, portValidator)
  }

  val mockServerAddress = ServerAddress("", 0)

  "A ServerAddressValidator" should {

    "use the underlaying validators" in {
      for {
        validIp <- List(true, false)
        validHostname <- List(true, false)
        portValidation <- List(ValidPort, NegativePort, ReservedPort, DynamicPort)
      } {
        val validator = genValidator(validIp, validHostname, portValidation)
        val response = validator.validateServerAddress(mockServerAddress)

        (portValidation, validIp, validHostname) match {
          case (ValidPort, vip, vh) if vip || vh =>
            response.isDefined must be(false)
          case _ =>
            response.isDefined must be(true)
        }
      }
    }

  }

}

class PortValidatorSpec extends WordSpec with MustMatchers {
  "A PortValidator" should {
    "validate that the ports are correct" in {
      val validator = PortValidator()
      validator.isValid(Int.MinValue) must be(PortValidation.NegativePort)
      validator.isValid(-3457) must be(PortValidation.NegativePort)
      validator.isValid(-1) must be(PortValidation.NegativePort)

      validator.isValid(0) must be(PortValidation.ReservedPort)
      validator.isValid(512) must be(PortValidation.ReservedPort)
      validator.isValid(1023) must be(PortValidation.ReservedPort)

      validator.isValid(1024) must be(PortValidation.ValidPort)
      validator.isValid(8080) must be(PortValidation.ValidPort)
      validator.isValid(49151) must be(PortValidation.ValidPort)

      validator.isValid(49152) must be(PortValidation.DynamicPort)
      validator.isValid(4535323) must be(PortValidation.DynamicPort)
      validator.isValid(Int.MaxValue) must be(PortValidation.DynamicPort)
    }
  }
}
