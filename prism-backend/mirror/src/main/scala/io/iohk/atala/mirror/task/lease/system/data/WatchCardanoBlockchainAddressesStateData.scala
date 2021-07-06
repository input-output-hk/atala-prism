package io.iohk.atala.mirror.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.mirror.models.CardanoBlockId
import CardanoBlockId._

case class WatchCardanoBlockchainAddressesStateData(lastCheckedBlockId: CardanoBlockId)

object WatchCardanoBlockchainAddressesStateData {
  implicit val watchCardanoBlockchainAddressesStateDataEncoder: Encoder[WatchCardanoBlockchainAddressesStateData] =
    deriveEncoder
  implicit val watchCardanoBlockchainAddressesStateDataDecoder: Decoder[WatchCardanoBlockchainAddressesStateData] =
    deriveDecoder
}
