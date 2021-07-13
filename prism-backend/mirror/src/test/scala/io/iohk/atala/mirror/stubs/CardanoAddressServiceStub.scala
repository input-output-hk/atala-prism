package io.iohk.atala.mirror.stubs

import io.iohk.atala.mirror.services.CardanoAddressService
import io.iohk.atala.mirror.models.{CardanoAddressKey, CardanoAddress}
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.stubs.CardanoAddressServiceStub.CardanoAddressFixtures

class CardanoAddressServiceStub(
    generateAddressKey: Either[CardanoAddressService.CardanoAddressServiceError, CardanoAddressKey] = Right(
      CardanoAddressFixtures.cardanoAddressKey
    )
) extends CardanoAddressService {

  override def generateAddressKey(
      extendedPublicKey: String,
      path: String
  ): Either[CardanoAddressService.CardanoAddressServiceError, CardanoAddressKey] = generateAddressKey

  override def generateWalletAddresses(
      extendedPublicKey: String,
      fromSequenceNo: Int,
      untilSequenceNo: Int,
      network: String
  ): Either[CardanoAddressService.CardanoAddressServiceError, List[(CardanoAddress, Int)]] =
    Right((fromSequenceNo until untilSequenceNo).map(id => CardanoAddress(s"addr_$id") -> id).toList)

  override def generateWalletAddress(
      extendedPublicKey: String,
      index: Int,
      network: String
  ): Either[CardanoAddressService.CardanoAddressServiceError, CardanoAddress] =
    Right(CardanoAddress(s"addr_$index"))

}

object CardanoAddressServiceStub extends MirrorFixtures
