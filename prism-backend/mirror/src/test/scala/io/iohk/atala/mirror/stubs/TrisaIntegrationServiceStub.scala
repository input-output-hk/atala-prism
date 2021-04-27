package io.iohk.atala.mirror.stubs

import io.iohk.atala.mirror.models.{CardanoAddress, LovelaceAmount, TrisaVaspAddress}
import io.iohk.atala.mirror.protos.ivms101.Person
import io.iohk.atala.mirror.services.TrisaIntegrationService
import io.iohk.atala.mirror.services.TrisaIntegrationService.TrisaError
import monix.eval.Task

class TrisaIntegrationServiceStub extends TrisaIntegrationService {
  override def initiateTransaction(
      source: CardanoAddress,
      destination: CardanoAddress,
      lovelaceAmount: LovelaceAmount,
      trisaVaspAddress: TrisaVaspAddress
  ): Task[Either[TrisaError, Person]] = Task.pure(Right(Person()))
}
