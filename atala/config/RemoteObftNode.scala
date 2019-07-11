package atala.config

import io.iohk.multicrypto._
import pureconfig.generic.semiauto._
import pureconfig.ConfigReader

case class RemoteObftNode(
    serverIndex: Int, // AKA 'i'
    publicKey: SigningPublicKey,
    address: ServerAddress
)

object RemoteObftNode {

  private val semiautoConfigReader: ConfigReader[RemoteObftNode] = deriveReader[RemoteObftNode]

  implicit val configReader: ConfigReader[RemoteObftNode] =
    semiautoConfigReader

}
