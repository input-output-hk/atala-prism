package io.iohk.connector.client

import io.iohk.connector.client.commands.{Command, UnitCommand}
import monocle.POptional
import scopt.OParser

object Config {
  val parserBuilder = OParser.builder[Config]

  /** Util to use monocle Optional optics to update fields in scopt config
    *
    * Scalaopt options require providing function (Value, Config) => Config to apply user provided value
    * to the config. This is quite mundane, especially for nested values. Lenses / other optics
    * come to help.
    *
    * Optional is an optics that points to a possible field. Reading from it returns Option[T]
    * where T is the underlying type, while writing modifies the field iff it is present.
    */
  implicit class OptifyHelper[ConfigType, FieldType](
      optional: POptional[ConfigType, ConfigType, FieldType, FieldType]
  ) {

    /** Sets the field to value provided by user */
    def optify: (FieldType, ConfigType) => ConfigType = { (value: FieldType, c: ConfigType) =>
      optional.set(value)(c)
    }

    /** Sets the field to transformation of value provided by user */
    def optify[T](f: T => FieldType): (T, ConfigType) => ConfigType = { (value: T, c: ConfigType) =>
      optional.set(f(value))(c)
    }
  }
}

case class Config(
    command: Command = UnitCommand,
    host: String = "localhost",
    port: Int = 50051,
    userId: Option[String] = None,
    pubKeyX: Option[String] = None,
    pubKeyY: Option[String] = None,
    encodedPublicKey: Option[String] = None,
    connectionToken: Option[String] = None,
    base64Message: Option[String] = None
)
