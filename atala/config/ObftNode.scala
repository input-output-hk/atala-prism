package atala.config

import scala.concurrent.duration._
import io.iohk.multicrypto._
import pureconfig.generic.semiauto._
import pureconfig.ConfigReader

case class ObftNode(
    serverIndex: Int, // AKA 'i'
    publicKey: SigningPublicKey,
    privateKey: SigningPrivateKey,
    database: String,
    remoteNodes: Set[RemoteObftNode],
    timeSlotDuration: FiniteDuration,
    address: ServerAddress
) {

  lazy val keyPair: SigningKeyPair = SigningKeyPair(publicKey, privateKey)

  lazy val addressesOfRemoteNodes: Set[(Int, ServerAddress)] =
    remoteNodes.map(s => (s.serverIndex, s.address))
  lazy val indexesOfRemoteNodes: Set[Int] =
    remoteNodes.map(_.serverIndex)
  private lazy val asRemoteNode: RemoteObftNode =
    RemoteObftNode(serverIndex, publicKey, address)
  private lazy val allNodes: Set[RemoteObftNode] =
    remoteNodes + this.asRemoteNode
  lazy val genesisKeys: List[SigningPublicKey] =
    allNodes.toList
      .sortBy(_.serverIndex)
      .map(_.publicKey)

}

object ObftNode {

  private val semiautoConfigReader: ConfigReader[ObftNode] = deriveReader[ObftNode]
  private val validator: ObftNodeValidator = ObftNodeValidator()

  implicit val configReader: ConfigReader[ObftNode] =
    semiautoConfigReader.emap { candidate =>
      validator.validateObftNode(candidate) match {
        case None => Right(candidate)
        case Some(error) => Left(error)
      }
    }

}
