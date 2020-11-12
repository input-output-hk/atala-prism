package io.iohk.atala.prism.node.services

import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.operations.{CreateDIDOperation, CreateDIDOperationSpec, TimestampInfo}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class DIDDataServiceSpec extends PostgresRepositorySpec {
  import CreateDIDOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val didDataService = new DIDDataService(didDataRepository)
  val dummyTimestamp = TimestampInfo.dummyTime

  "DIDDataServiceSpec.findByDID" should {
    "retrieve DID document from database" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyTimestamp).toOption.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val did = DID.buildPrismDID(parsedOperation.id.suffix)
      didDataService.findByDID(did).value.futureValue mustBe a[Right[_, _]]
    }

    "return error when did is in invalid format" in {
      val did = DID.buildPrismDID("1111111111111111")
      didDataService.findByDID(did).value.futureValue mustBe a[Left[UnknownValueError, _]]
    }

  }

}
