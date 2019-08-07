package atala.apps.kvnode

import atala.config._
import pureconfig.generic.semiauto._
import pureconfig.ConfigReader

case class KvConfig(obft: ObftNode, restAddress: ServerAddress)

object KvConfig {

  private val semiautoConfigReader: ConfigReader[KvConfig] = deriveReader[KvConfig]

  implicit val configReader: ConfigReader[KvConfig] =
    semiautoConfigReader

}
