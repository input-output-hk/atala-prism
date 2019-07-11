package atala.config

import pureconfig.generic.semiauto._
import pureconfig.ConfigReader

case class ServerAddress(host: String, port: Int)

object ServerAddress {

  private val semiautoConfigReader: ConfigReader[ServerAddress] = deriveReader[ServerAddress]
  private val validator: ServerAddressValidator = ServerAddressValidator()

  implicit val configReader: ConfigReader[ServerAddress] =
    semiautoConfigReader.emap { candidate =>
      validator.validateServerAddress(candidate) match {
        case None => Right(candidate)
        case Some(error) => Left(error)
      }
    }
}
